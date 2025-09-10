# Dockerfile
# Usa MySQL 8.4 (LTS). También puedes usar mysql:8.0.36 o superior.
FROM mysql:8.4

# Copia tu init.sql (tu script con CREATE DATABASE/TABLE e INSERTs)
# Se ejecuta SOLO si /var/lib/mysql está vacío.
COPY init.sql /docker-entrypoint-initdb.d/01-init.sql

EXPOSE 3306