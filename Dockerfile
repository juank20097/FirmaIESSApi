# ============================================================
#  transversal-firmaec-api - Dockerfile
#  Autor: Juan Carlos Estevez Hidalgo
#  Fecha: 07 may 2026
#
#  Multi-stage build:
#    Stage 1 (builder) -> Compila el proyecto con Maven.
#    Stage 2 (runtime) -> Imagen final optimizada con JRE.
#
#  Uso:
#    Construir: docker build -t transversal-firmaec-api:2.0.0 .
#    Ejecutar:  docker run -p 8090:8090 --env-file .env transversal-firmaec-api:2.0.0
# ============================================================

# ─────────────────────────────────────────────────────────────
# Stage 1 - Builder
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia archivos necesarios para aprovechar la cache de Maven
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw

# Descarga dependencias
RUN ./mvnw dependency:go-offline -B

# Copia codigo fuente
COPY src/ src/

# Compila la aplicacion
RUN ./mvnw clean package -DskipTests -B


# ─────────────────────────────────────────────────────────────
# Stage 2 - Runtime
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="Juan Carlos Estevez Hidalgo"
LABEL application="transversal-firmaec-api"
LABEL version="2.0.0"

# Usuario sin privilegios
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup

WORKDIR /app

# Directorio para almacenamiento local
RUN mkdir -p uploads && \
    chown -R appuser:appgroup /app

# Copia unicamente el artefacto compilado
COPY --from=builder /app/target/FirmaIessApi-1.0.0-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8090

# Parametros de la JVM optimizados para contenedores
ENV JAVA_OPTS="\
-XX:+UseContainerSupport \
-XX:MaxRAMPercentage=75.0 \
-XX:+UseG1GC \
-Djava.security.egd=file:/dev/urandom \
-Duser.timezone=America/Guayaquil"

# Inicio de la aplicacion
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
