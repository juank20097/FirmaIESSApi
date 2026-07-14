# ============================================================
#  base-spring-api - Dockerfile
#  Autor: Juan Carlos Estévez Hidalgo
#  Fecha: 07 may 2026
#
#  Multi-stage build:
#    Stage 1 (builder) → Compila el proyecto con Maven.
#    Stage 2 (runtime) → Imagen final optimizada con JRE.
#
#  Uso:
#    Construir: docker build -t base-spring-api:1.0.0 .
#    Ejecutar:  docker run -p 8080:8080 --env-file .env base-spring-api:1.0.0
# ============================================================

# ─────────────────────────────────────────────────────────────
# Stage 1 - Builder
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copia archivos necesarios para aprovechar la caché de Maven
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw

# Descarga dependencias
RUN ./mvnw dependency:go-offline -B

# Copia código fuente
COPY src/ src/

# Compila la aplicación
RUN ./mvnw clean package -DskipTests -B


# ─────────────────────────────────────────────────────────────
# Stage 2 - Runtime
# ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="Juan Carlos Estévez Hidalgo"
LABEL application="base-spring-api"
LABEL version="1.0.0-SNAPSHOT"

# Usuario sin privilegios
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup

WORKDIR /app

# Directorio para almacenamiento local
RUN mkdir -p uploads && \
    chown -R appuser:appgroup /app

# Copia únicamente el artefacto compilado
COPY --from=builder /app/target/base-spring-api-1.0.0-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8080

# Parámetros de la JVM optimizados para contenedores
ENV JAVA_OPTS="\
-XX:+UseContainerSupport \
-XX:MaxRAMPercentage=75.0 \
-XX:+UseG1GC \
-Djava.security.egd=file:/dev/./urandom \
-Duser.timezone=America/Guayaquil"

# Inicio de la aplicación
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]