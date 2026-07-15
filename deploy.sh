#!/bin/bash
# =============================================================================
# PAS-EST-061 v1.0 — deploy.sh
# Proyecto   : base-spring-api
# Equipo     : Subdirección Nacional de Arquitectura y Soluciones (SDNAS)
#
# PROPÓSITO (§5.11.2):
#   Automatizar y estandarizar el despliegue local durante el flujo de
#   transición (mientras no existan pipelines CI/CD completamente integrados).
#   También válido para: validación técnica, pruebas de instalación y
#   verificaciones rápidas de infraestructura.
#
# USO:
#   ./deploy.sh                  → rama actual, git pull + build local + despliegue
#   ./deploy.sh Desarrollo        → cambia a rama Desarrollo antes del pull
#   ./deploy.sh Certificacion     → cambia a rama Certificacion antes del pull
#   ./deploy.sh --no-build        → git pull + despliegue usando imagen ya existente
#   ./deploy.sh --no-pull         → omite git pull (usa el código ya presente)
#
# ALCANCE:
#   Este script administra TANTO el ecosistema de utilitarios
#   (docker-compose-herramientas.yml: Vault, Postgres, MongoDB, MinIO) COMO
#   la aplicación (docker-compose.yml). En CADA ejecución, ambos conjuntos
#   de contenedores se detienen y se vuelven a levantar desde cero. Los
#   volúmenes nombrados (datos de Postgres, Mongo, MinIO, uploads) NO se
#   eliminan, por lo que la información persiste entre reinicios.
#
# RESPONSABILIDADES
#   1.  Validar prerrequisitos técnicos del entorno anfitrión
#   2.  Actualizar el código fuente (git pull) desde el repositorio GitLab
#   3.  Reiniciar el ecosistema de utilitarios (down + up)
#   4.  Reiniciar los contenedores de la app (down) antes de reconstruir
#   5.  Validar variables de entorno requeridas (.env)
#   6.  Crear y validar redes de contenedores
#   7.  Crear y validar volúmenes persistentes
#   8.  Obtener/construir la imagen (Harbor si está configurado, o build local)
#   9.  Validar sintaxis del archivo docker-compose.yml
#   10. Ejecutar el despliegue de la app con Docker Compose (up)
#   11. Verificar health checks de los contenedores desplegados
#   12. Resumen final / Ejecutar rollback automático ante fallos
#
# RESTRICCIONES (§5.11.2):
#   - Este script NO ejecuta ningún script de arranque del contenedor
#     directamente. El ENTRYPOINT/CMD definido en el Dockerfile es quien
#     arranca la aplicación (java -jar app.jar) dentro del contenedor.
#   - Este script NO ejecuta DDL de base de datos (CREATE TABLE, ALTER, etc.).
#     La gestión de esquema la realiza Hibernate (DB_*_DDL_AUTO) o scripts
#     de migración fuera de este flujo. Las credenciales y conexión a BD
#     se resuelven vía Vault/.env, nunca se hardcodean aquí.
# =============================================================================

set -euo pipefail

# =============================================================================
# CONFIGURACIÓN — ajustar por proyecto
# =============================================================================
readonly PROJECT_NAME="transversal-firmaec-api"
readonly COMPOSE_FILE="docker-compose.yml"
readonly UTILS_COMPOSE_FILE="docker-compose-herramientas.yml"
readonly ENV_FILE=".env"
readonly SERVICE_NAME="svc-transversal-firmaec-api"

# ── Access token GitLab ──────────────────────────────────────────────────
# Reemplazar con el personal access token de GitLab antes de ejecutar.
# NO versionar este valor en Git (.gitignore ya excluye deploy.sh).
GITLAB_TOKEN=""

# Nombres de proyecto Compose fijos y EXPLICITOS.
# Docker Compose v2 infiere el nombre de proyecto a partir del nombre del
# directorio actual cuando no se especifica con -p/--project-name. Si el
# script se ejecuta desde distintas carpetas (p.ej. ~/docker/spring vs
# ~/basespringapi), ambos compose pueden terminar bajo el MISMO nombre de
# proyecto inferido, y entonces 'down --remove-orphans' de uno arrastra
# contenedores del otro. Fijar -p evita esa ambiguedad sin importar desde
# que ruta se invoque deploy.sh.
readonly APP_PROJECT="transversal-firmaec-api-app"
readonly UTILS_PROJECT="transversal-firmaec-api-utils"

# Wrappers para no repetir -p en cada invocacion de docker compose.
dc_app()   { docker compose -p "${APP_PROJECT}" -f "${COMPOSE_FILE}" "$@"; }
dc_utils() { docker compose -p "${UTILS_PROJECT}" -f "${UTILS_COMPOSE_FILE}" "$@"; }

# Redes (PAS-EST-059: prefijo net-). net-iess es creada por
# docker-compose-herramientas.yml y consumida como 'external' por
# docker-compose.yml.
readonly NETWORKS=(
    "net-iess"
)

# Volúmenes nombrados por Docker (PAS-EST-059: prefijo vol-)
readonly VOLUMES=(
    "vol-transversal-firmaec-api-uploads"
)

# Imagen principal desplegada (para build local y rollback)
readonly IMAGE_TAG="transversal-firmaec-api:2.0.0"
# Si en el futuro se publica en Harbor, descomentar y ajustar:
# readonly HARBOR_IMAGE="harbor.iess.gob.ec/dnti/transversal/base-spring-api:1.0.0"

# Tiempo de espera para que los utilitarios (Vault, Postgres, Mongo, MinIO)
# alcancen estado healthy antes de continuar con el despliegue de la app.
readonly UTILS_HEALTHCHECK_WAIT=10
readonly UTILS_HEALTHCHECK_RETRIES=10
readonly UTILS_HEALTHCHECK_INTERVAL=10

# Tiempo de espera para health checks de la app tras levantar el contenedor
readonly HEALTHCHECK_WAIT=20
readonly HEALTHCHECK_RETRIES=10
readonly HEALTHCHECK_INTERVAL=10

# Rama de Git a actualizar: se detecta automáticamente la rama en la que
# está parado el working copy en el momento de ejecutar el script.
# (no se fija una rama fija como 'main' porque el despliegue puede
# ejecutarse desde cualquier rama, p.ej. Desarrollo, QA, etc.)

# Flags de ejecución (default: hacer pull y construir localmente)
DO_BUILD=true
DO_PULL=true
TARGET_BRANCH=""
for ARG in "$@"; do
    case "${ARG}" in
        --no-build) DO_BUILD=false ;;
        --no-pull)  DO_PULL=false ;;
        --*)        ;; # ignorar flags desconocidos
        *)          TARGET_BRANCH="${ARG}" ;; # cualquier otro argumento es la rama
    esac
done

# =============================================================================
# UTILIDADES DE SALIDA
# =============================================================================
log_step()  { echo ""; echo "──────────────────────────────────────────────"; echo " $1"; echo "──────────────────────────────────────────────"; }
log_ok()    { echo "    ✔  $1"; }
log_warn()  { echo "    ⚠  ADVERTENCIA: $1"; }
log_error() { echo "    ✘  ERROR: $1" >&2; }
log_info()  { echo "    →  $1"; }

# =============================================================================
# ROLLBACK AUTOMÁTICO
# Se activa mediante trap ante cualquier error no controlado (ERR).
# Detiene los contenedores de la APP levantados en el intento fallido y,
# si existe una imagen previa válida, la restaura. No toca los utilitarios.
# =============================================================================
PREVIOUS_IMAGE=""
CURRENT_IMAGE=""

rollback() {
    local exit_code=$?
    echo ""
    log_error "El despliegue falló con código de salida ${exit_code}."
    log_step "ROLLBACK AUTOMÁTICO"
    log_info "Deteniendo contenedores de la app del despliegue fallido..."
    docker compose -p "${APP_PROJECT}" -f "${COMPOSE_FILE}" down --remove-orphans 2>/dev/null || true

    if [ -n "${PREVIOUS_IMAGE}" ] && docker image inspect "${PREVIOUS_IMAGE}" >/dev/null 2>&1; then
        log_info "Restaurando imagen previa: ${PREVIOUS_IMAGE} → ${CURRENT_IMAGE}"
        docker tag "${PREVIOUS_IMAGE}" "${CURRENT_IMAGE}"
        log_info "Reiniciando con imagen restaurada..."
        docker compose -p "${APP_PROJECT}" -f "${COMPOSE_FILE}" up -d
        log_warn "Rollback completado. Verificar manualmente el estado del servicio."
    else
        log_warn "No se encontró imagen previa. No se puede restaurar automáticamente."
        log_warn "Intervención manual requerida."
    fi
    echo ""
}

trap rollback ERR

# =============================================================================
# CABECERA
# =============================================================================
echo ""
echo "============================================================"
echo "  IESS — DNTI / SDNAS"
echo "  Despliegue Local Estandarizado — PAS-EST-061"
echo "  Proyecto : ${PROJECT_NAME}"
echo "  Modo     : $([ "${DO_BUILD}" = true ] && echo 'build local' || echo 'sin build (imagen existente)')"
echo "  Git pull : $([ "${DO_PULL}" = true ] && echo 'si' || echo 'omitido (--no-pull)')"
echo "  Fecha    : $(date '+%Y-%m-%d %H:%M:%S %Z')"
echo "============================================================"

# =============================================================================
# PASO 1 — VALIDACIÓN DE PRERREQUISITOS
# Verificar que las herramientas requeridas estén instaladas y operativas.
# =============================================================================
log_step "PASO 1/11 — Validación de prerrequisitos"

if ! command -v docker >/dev/null 2>&1; then
    log_error "Docker Engine no encontrado. Instalar Docker Engine/Docker Desktop."
    exit 1
fi
DOCKER_VERSION=$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo "desconocida")
log_ok "Docker Engine v${DOCKER_VERSION} disponible"

if ! docker compose version >/dev/null 2>&1; then
    log_error "Docker Compose plugin no disponible. Requiere docker-compose-plugin v2.x."
    exit 1
fi
COMPOSE_VERSION=$(docker compose version --short 2>/dev/null || echo "desconocida")
log_ok "Docker Compose v${COMPOSE_VERSION} disponible"

if [ ! -f "${COMPOSE_FILE}" ]; then
    log_error "Archivo ${COMPOSE_FILE} no encontrado en $(pwd)."
    log_error "Ejecutar este script desde la raíz del proyecto."
    exit 1
fi
log_ok "Archivo ${COMPOSE_FILE} encontrado"

if [ ! -f "${UTILS_COMPOSE_FILE}" ]; then
    log_error "Archivo ${UTILS_COMPOSE_FILE} no encontrado en $(pwd)."
    exit 1
fi
log_ok "Archivo ${UTILS_COMPOSE_FILE} encontrado"

if [ "${DO_PULL}" = true ] && ! command -v git >/dev/null 2>&1; then
    log_error "Git no encontrado en el entorno anfitrión. Requerido para actualizar el código."
    exit 1
fi

# =============================================================================
# PASO 2 — ACTUALIZACIÓN DEL CÓDIGO FUENTE (GIT PULL)
# Actualiza la copia local del repositorio antes de construir la imagen.
# Solo aplica un 'pull' sobre el working copy ya clonado; este script no
# clona el repositorio si no existe (uso exclusivo de entornos ya provistos
# mediante el flujo de clonado inicial documentado aparte).
# =============================================================================
log_step "PASO 2/12 — Actualización del código fuente (git pull)"

if [ "${DO_PULL}" = false ]; then
    log_warn "git pull omitido por flag --no-pull. Se usará el código ya presente."
elif [ ! -d ".git" ]; then
    log_warn "No se encontró un repositorio Git (.git) en $(pwd)."
    log_warn "Se omite la actualización de código. Verificar manualmente el origen del código fuente."
else
    # Cambiar de rama si se pasó como argumento
    if [ -n "${TARGET_BRANCH}" ]; then
        log_info "Rama solicitada: ${TARGET_BRANCH}"
        if ! git checkout "${TARGET_BRANCH}" 2>/dev/null; then
            log_error "No se pudo cambiar a la rama '${TARGET_BRANCH}'. Verificar que exista en el repositorio."
            exit 1
        fi
        log_ok "Cambiado a rama '${TARGET_BRANCH}'"
    fi

    GIT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
    if [ -z "${GIT_BRANCH}" ] || [ "${GIT_BRANCH}" = "HEAD" ]; then
        log_error "No se pudo determinar la rama actual (¿HEAD detached?). Verificar manualmente con 'git status'."
        exit 1
    fi
    log_info "Rama activa: ${GIT_BRANCH}"

    if [ -n "$(git status --porcelain 2>/dev/null)" ]; then
        log_warn "Existen cambios locales sin confirmar. git pull podría fallar o generar conflictos."
    fi

    # Construir URL con access token si está configurado
    GIT_REMOTE_URL=$(git remote get-url origin 2>/dev/null || echo "")
    if [ -n "${GITLAB_TOKEN}" ] && [ -n "${GIT_REMOTE_URL}" ]; then
        # Inyectar oauth2:TOKEN en la URL: https://gitlab... -> https://oauth2:TOKEN@gitlab...
        GIT_PULL_URL=$(echo "${GIT_REMOTE_URL}" | sed "s|https://|https://oauth2:${GITLAB_TOKEN}@|")
        log_info "Actualizando rama '${GIT_BRANCH}' con access token..."
        if ! git pull "${GIT_PULL_URL}" "${GIT_BRANCH}"; then
            log_error "Fallo al ejecutar git pull sobre la rama '${GIT_BRANCH}'."
            exit 1
        fi
    else
        log_info "Actualizando rama '${GIT_BRANCH}' desde el remoto (git pull)..."
        if ! git pull origin "${GIT_BRANCH}"; then
            log_error "Fallo al ejecutar git pull sobre la rama '${GIT_BRANCH}'."
            log_error "Si requiere autenticación, configure GITLAB_TOKEN en deploy.sh."
            exit 1
        fi
    fi

    GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "desconocido")
    log_ok "Código actualizado correctamente. Commit actual: ${GIT_COMMIT}"
fi

# =============================================================================
# PASO 3 — ECOSISTEMA DE UTILITARIOS
# Reinicia siempre docker-compose-herramientas.yml: detiene los contenedores
# existentes (si los hay) y los vuelve a levantar desde cero. Este archivo
# crea la red net-iess (consumida como 'external' por docker-compose.yml) y
# los servicios de infraestructura: Vault, Postgres, MongoDB, MinIO.
# No se ejecuta DDL de base de datos aquí — cada motor inicializa su propio
# volumen mediante sus mecanismos nativos. Los volúmenes nombrados (Postgres,
# Mongo, MinIO) NO se eliminan en este paso, por lo que los datos persisten
# entre reinicios.
# =============================================================================
log_step "PASO 3/12 — Ecosistema de utilitarios (Vault / BD / Mongo / MinIO)"

UTILS_RUNNING=$(dc_utils ps -q 2>/dev/null | wc -l | tr -d ' ' || true)
UTILS_RUNNING="${UTILS_RUNNING:-0}"

if [ "${UTILS_RUNNING}" -gt 0 ]; then
    log_info "Contenedores de utilitarios detectados (${UTILS_RUNNING}). Deteniendo para reiniciar..."
    dc_utils down --remove-orphans
    log_ok "Contenedores de utilitarios previos eliminados"
else
    log_ok "Sin contenedores de utilitarios previos que limpiar"
fi

if ! docker network inspect net-iess >/dev/null 2>&1; then
    docker network create net-iess >/dev/null
    log_ok "Red 'net-iess' creada"
else
    log_ok "Red 'net-iess' ya existe"
fi

log_info "Levantando ecosistema de utilitarios (${UTILS_COMPOSE_FILE})..."
if ! dc_utils up -d; then
    log_error "Fallo al levantar el ecosistema de utilitarios."
    exit 1
fi
log_ok "Ecosistema de utilitarios iniciado"

log_info "Esperando ${UTILS_HEALTHCHECK_WAIT}s antes de verificar salud de utilitarios..."
sleep "${UTILS_HEALTHCHECK_WAIT}"

UTILS_SERVICES=$(dc_utils ps --services 2>/dev/null)
for USVC in ${UTILS_SERVICES}; do
    [ "${USVC}" = "svc-vault-init" ] && continue  # tarea de un solo uso, no persiste healthy

    ATTEMPT=0
    UTIL_HEALTHY=false
    while [ "${ATTEMPT}" -lt "${UTILS_HEALTHCHECK_RETRIES}" ]; do
        UCID=$(dc_utils ps -q "${USVC}" 2>/dev/null | head -1)
        [ -z "${UCID}" ] && break

        USTATUS=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' "${UCID}" 2>/dev/null || echo "error")
        URUNNING=$(docker inspect --format='{{.State.Status}}' "${UCID}" 2>/dev/null || echo "error")

        if [ "${USTATUS}" = "healthy" ] || { [ "${USTATUS}" = "no-healthcheck" ] && [ "${URUNNING}" = "running" ]; }; then
            log_ok "  ${USVC} → ${USTATUS} ✔"
            UTIL_HEALTHY=true
            break
        fi

        ATTEMPT=$((ATTEMPT + 1))
        log_info "  ${USVC} → ${USTATUS} (intento ${ATTEMPT}/${UTILS_HEALTHCHECK_RETRIES})..."
        sleep "${UTILS_HEALTHCHECK_INTERVAL}"
    done

    if [ "${UTIL_HEALTHY}" = false ]; then
        log_error "El servicio de utilitario '${USVC}' no alcanzó estado healthy."
        log_info "Logs de ${USVC}:"
        dc_utils logs --tail=20 "${USVC}" 2>/dev/null || true
        exit 1
    fi
done

if ! docker network inspect net-iess >/dev/null 2>&1; then
    log_error "La red 'net-iess' no fue creada por ${UTILS_COMPOSE_FILE}."
    exit 1
fi
log_ok "Red 'net-iess' disponible"

# =============================================================================
# PASO 4 — LIMPIEZA PREVIA (de la app, no de los utilitarios)
# Eliminar contenedores detenidos y capas de imagen sin etiqueta que puedan
# interferir con el despliegue actual. Se preservan imágenes con tag SemVer.
# =============================================================================
log_step "PASO 4/12 — Limpieza de artefactos residuales"

APP_CONTAINERS=$(dc_app ps -q 2>/dev/null || true)
if [ -n "${APP_CONTAINERS}" ]; then
    log_info "Contenedores previos detectados. Deteniendo..."

    CURRENT_IMAGE="${IMAGE_TAG}"
    RUNNING_CONTAINER=$(dc_app ps -q "${SERVICE_NAME}" 2>/dev/null | head -1 || true)
    if [ -n "${RUNNING_CONTAINER}" ]; then
        PREVIOUS_IMAGE=$(docker inspect --format='{{.Config.Image}}' "${RUNNING_CONTAINER}" 2>/dev/null || echo "")
        log_info "Imagen previa registrada para rollback: ${PREVIOUS_IMAGE:-ninguna}"
    fi

    dc_app down --remove-orphans
    log_ok "Contenedores previos eliminados"
else
    log_ok "Sin contenedores previos que limpiar"
fi

DANGLING=$(docker images -f "dangling=true" -q 2>/dev/null || echo "")
if [ -n "${DANGLING}" ]; then
    echo "${DANGLING}" | xargs docker rmi 2>/dev/null || true
    log_ok "Imágenes intermedias sin etiqueta eliminadas"
else
    log_ok "Sin imágenes dangling que limpiar"
fi

# =============================================================================
# PASO 4 — VALIDACIÓN DE VARIABLES DE ENTORNO
# El archivo .env no debe estar versionado en Git (.gitignore).
# Permisos estrictos: 600 (solo lectura/escritura para el propietario).
# Las credenciales sensibles (BD, Vault, MinIO) se resuelven en runtime
# vía Vault — aquí solo se valida la presencia de las claves obligatorias
# de control, no sus valores secretos.
# =============================================================================
log_step "PASO 5/12 — Validación de variables de entorno"

if [ ! -f "${ENV_FILE}" ]; then
    log_error "Archivo ${ENV_FILE} no encontrado."
    log_error "Copiar la plantilla: cp .env.example .env"
    exit 1
fi

chmod 600 "${ENV_FILE}"
log_ok "Archivo ${ENV_FILE} encontrado y permisos aplicados (600)"

REQUIRED_VARS=(
    "DB_ENGINE"
    "SERVER_PORT"
    "VAULT_ENABLED"
    "VAULT_HOST"
)

MISSING_VARS=0
for VAR in "${REQUIRED_VARS[@]}"; do
    if ! grep -q "^${VAR}=" "${ENV_FILE}"; then
        log_error "Variable obligatoria ausente en ${ENV_FILE}: ${VAR}"
        MISSING_VARS=$((MISSING_VARS + 1))
    fi
done

if [ "${MISSING_VARS}" -gt 0 ]; then
    log_error "${MISSING_VARS} variable(s) obligatoria(s) ausente(s). Verificar ${ENV_FILE}."
    exit 1
fi
log_ok "Todas las variables de control obligatorias presentes en ${ENV_FILE}"

if grep -q "^VAULT_ENABLED=false" "${ENV_FILE}"; then
    log_warn "VAULT_ENABLED=false: las credenciales marcadas [VAULT] deben estar"
    log_warn "completadas manualmente en ${ENV_FILE} (DB, MinIO, Mongo)."
fi

# =============================================================================
# PASO 5 — CREACIÓN Y VALIDACIÓN DE REDES
# Validación final/defensiva. La red ya debió quedar creada en el PASO 2.
# =============================================================================
log_step "PASO 6/12 — Creación y validación de redes"

for NETWORK in "${NETWORKS[@]}"; do
    if docker network inspect "${NETWORK}" >/dev/null 2>&1; then
        log_ok "Red '${NETWORK}' ya existe"
    else
        docker network create "${NETWORK}" >/dev/null
        log_ok "Red '${NETWORK}' creada"
    fi
done

# =============================================================================
# PASO 6 — CREACIÓN Y VALIDACIÓN DE VOLÚMENES
# Volúmenes nombrados por Docker para persistencia de archivos locales
# (cuando MINIO_ENABLED=false, los documentos se guardan en este volumen).
# =============================================================================
log_step "PASO 7/12 — Creación y validación de volúmenes"

for VOL in "${VOLUMES[@]}"; do
    if docker volume inspect "${VOL}" >/dev/null 2>&1; then
        log_ok "Volumen '${VOL}' ya existe"
    else
        docker volume create "${VOL}" >/dev/null
        log_ok "Volumen '${VOL}' creado"
    fi
done

# =============================================================================
# PASO 7 — OBTENCIÓN DE LA IMAGEN
# Mientras el proyecto no esté publicado en Harbor, la imagen se construye
# localmente a partir del Dockerfile (multi-stage). Cuando exista publicación
# en Harbor con etiqueta SemVer, reemplazar este bloque por:
#   docker compose -f "${COMPOSE_FILE}" pull
# Prohibido usar 'latest' o etiquetas ambiguas (§5.3).
# =============================================================================
log_step "PASO 8/12 — Obtención de la imagen"

if [ "${DO_BUILD}" = true ]; then
    log_info "Construyendo imagen local ${IMAGE_TAG} desde Dockerfile..."
    if ! dc_app build; then
        log_error "Fallo al construir la imagen ${IMAGE_TAG}."
        exit 1
    fi
    log_ok "Imagen ${IMAGE_TAG} construida exitosamente"
else
    log_info "Modo --no-build: se omite la construcción/pull de imagen."
    if ! docker image inspect "${IMAGE_TAG}" >/dev/null 2>&1; then
        log_error "La imagen ${IMAGE_TAG} no existe localmente y se solicitó --no-build."
        exit 1
    fi
    log_ok "Imagen ${IMAGE_TAG} encontrada localmente"
fi

# =============================================================================
# PASO 8 — VALIDACIÓN SINTÁCTICA DEL COMPOSE
# Detecta errores de sintaxis YAML y referencias a variables no definidas
# antes de intentar el despliegue.
# =============================================================================
log_step "PASO 9/12 — Validación sintáctica del archivo Compose"

if ! dc_app config >/dev/null 2>&1; then
    log_error "El archivo ${COMPOSE_FILE} contiene errores sintácticos o referencias inválidas."
    log_info "Ejecutar 'docker compose config' para ver el detalle."
    exit 1
fi
log_ok "Sintaxis de ${COMPOSE_FILE} validada correctamente"

# =============================================================================
# PASO 9 — DESPLIEGUE (§5.11.2)
# Levantar el servicio definido en docker-compose.yml en segundo plano.
# El ENTRYPOINT del contenedor (definido en el Dockerfile) arranca
# automáticamente la aplicación; este script no lo invoca directamente.
# =============================================================================
log_step "PASO 10/12 — Ejecutando despliegue de la app"

log_info "Iniciando servicio con Docker Compose..."
dc_app up -d
log_ok "Servicio iniciado en segundo plano"

# =============================================================================
# PASO 10 — VERIFICACIÓN DE HEALTH CHECKS
# Esperar a que el contenedor de la app alcance estado 'healthy' o 'running'.
# =============================================================================
log_step "PASO 11/12 — Verificación de health checks"

log_info "Esperando ${HEALTHCHECK_WAIT}s para que el contenedor inicialice..."
sleep "${HEALTHCHECK_WAIT}"

ALL_HEALTHY=true
SERVICES=$(dc_app ps --services 2>/dev/null)

for SERVICE in ${SERVICES}; do
    log_info "Verificando servicio: ${SERVICE}"
    ATTEMPT=0
    SERVICE_HEALTHY=false

    while [ "${ATTEMPT}" -lt "${HEALTHCHECK_RETRIES}" ]; do
        CONTAINER_ID=$(dc_app ps -q "${SERVICE}" 2>/dev/null | head -1)

        if [ -z "${CONTAINER_ID}" ]; then
            log_error "Contenedor del servicio '${SERVICE}' no encontrado."
            ALL_HEALTHY=false
            break
        fi

        HEALTH_STATUS=$(docker inspect \
            --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}no-healthcheck{{end}}' \
            "${CONTAINER_ID}" 2>/dev/null || echo "error")

        RUNNING_STATUS=$(docker inspect \
            --format='{{.State.Status}}' \
            "${CONTAINER_ID}" 2>/dev/null || echo "error")

        if [ "${HEALTH_STATUS}" = "healthy" ]; then
            log_ok "  ${SERVICE} → healthy ✔"
            SERVICE_HEALTHY=true
            break
        elif [ "${HEALTH_STATUS}" = "no-healthcheck" ] && [ "${RUNNING_STATUS}" = "running" ]; then
            log_ok "  ${SERVICE} → running (sin healthcheck configurado) ✔"
            SERVICE_HEALTHY=true
            break
        elif [ "${HEALTH_STATUS}" = "starting" ] || [ "${RUNNING_STATUS}" = "starting" ]; then
            ATTEMPT=$((ATTEMPT + 1))
            log_info "  ${SERVICE} → starting (intento ${ATTEMPT}/${HEALTHCHECK_RETRIES})..."
            sleep "${HEALTHCHECK_INTERVAL}"
        else
            log_error "  ${SERVICE} → ${HEALTH_STATUS} / ${RUNNING_STATUS}"
            log_info "  Últimas líneas de log de ${SERVICE}:"
            dc_app logs --tail=20 "${SERVICE}" 2>/dev/null || true
            ALL_HEALTHY=false
            break
        fi
    done

    if [ "${SERVICE_HEALTHY}" = "false" ] && [ "${ALL_HEALTHY}" = "true" ]; then
        log_error "  ${SERVICE} no alcanzó estado healthy tras $((HEALTHCHECK_RETRIES * HEALTHCHECK_INTERVAL))s."
        ALL_HEALTHY=false
    fi
done

if [ "${ALL_HEALTHY}" = "false" ]; then
    log_error "Uno o más servicios no pasaron el health check. Iniciando rollback."
    exit 1
fi

# =============================================================================
# PASO 11 — RESUMEN FINAL
# =============================================================================
log_step "PASO 12/12 — Resumen del despliegue"

echo ""
echo "  Estado de contenedores de utilitarios:"
dc_utils ps
echo ""
echo "  Estado de contenedores de la app:"
dc_app ps
echo ""
log_ok "Despliegue de '${PROJECT_NAME}' completado exitosamente"
log_info "API disponible en:        http://<IP_HOST>:8090/api"
log_info "Swagger UI disponible en: http://<IP_HOST>:8090/api/swagger-ui.html"
log_info "Health check:             http://<IP_HOST>:8090/api/actuator/health"
log_info "MinIO consola en:         http://<IP_HOST>:9001"
echo ""
echo "============================================================"
echo "  Despliegue finalizado: $(date '+%Y-%m-%d %H:%M:%S %Z')"
echo "============================================================"
echo ""
