FROM gradle:8.4-jdk17 AS builder

WORKDIR /build

# ===== PASO 1: Copiar archivos de configuracion de Gradle =====
# Configuracion principal del proyecto
COPY build.gradle .
COPY main.gradle .
COPY settings.gradle .
COPY gradle.properties .
COPY gradlew .
COPY gradle/ gradle/

# Configuraciones de modulos
COPY applications/app-service/build.gradle applications/app-service/
COPY domain/model/build.gradle domain/model/
COPY domain/usecase/build.gradle domain/usecase/
COPY infrastructure/*/build.gradle infrastructure/*/

# Descargar dependencias (se cachea mientras no cambien los build.gradle)
RUN gradle dependencies --no-daemon

# ===== PASO 2: Copiar codigo fuente completo =====
# Copiar toda la estructura de Clean Architecture
COPY applications/ applications/
COPY domain/ domain/
COPY infrastructure/ infrastructure/

# ===== PASO 3: Construir el JAR del app-service =====
RUN ./gradlew bootJar bootJar --no-daemon -x validateStructure

# ===== STAGE 2: RUNTIME =====
FROM openjdk:17-jdk-slim AS runtime

# Variables de entorno
ENV APP_HOME=/app
ENV APP_JAR=app-service.jar

# Instalar curl para health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Crear usuario no-root
RUN addgroup --system appuser && adduser --system --group appuser

WORKDIR $APP_HOME

# Copiar JAR desde build stage (estructura específica del scaffold)
COPY --from=builder /build/applications/app-service/build/libs/*.jar $APP_JAR

# Cambiar ownership
RUN chown -R appuser:appuser $APP_HOME

# Cambiar a usuario no-root
USER appuser

# Configuración JVM optimizada
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Exponer puerto (ajustar según tu configuración)
EXPOSE 8080

# Comando de inicio
CMD ["sh", "-c", "java $JAVA_OPTS -jar $APP_JAR"]