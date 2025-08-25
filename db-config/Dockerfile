# Dockerfile
# Usa MySQL 8.4 (LTS). También puedes usar mysql:8.0.36 o superior.
FROM mysql:8.4

# (Opcional) Variables por defecto SOLO para desarrollo.
# En producción, pásalas en runtime (docker run / compose).
ENV MYSQL_ROOT_PASSWORD=root_pass_123 \
    MYSQL_DATABASE=autenticacion \
    MYSQL_USER=appuser \
    MYSQL_PASSWORD=app_pass_123 \
    TZ=America/Bogota

# Copia tu init.sql (tu script con CREATE DATABASE/TABLE e INSERTs)
# Se ejecuta SOLO si /var/lib/mysql está vacío.
COPY init.sql /docker-entrypoint-initdb.d/01-init.sql

EXPOSE 3306