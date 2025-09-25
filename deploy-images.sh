#!/bin/bash

# Configuración
ACCOUNT_ID="799722626455"
REGION="us-east-1"

echo "Creando repositorios ECR..."
aws ecr create-repository --repository-name crediya-dev-autenticacion --region $REGION 2>/dev/null || echo "Repositorio crediya-dev-autenticacion ya existe"
aws ecr create-repository --repository-name crediya-dev-solicitudes --region $REGION 2>/dev/null || echo "Repositorio crediya-dev-solicitudes ya existe"
aws ecr create-repository --repository-name crediya-dev-reportes --region $REGION 2>/dev/null || echo "Repositorio crediya-dev-reportes ya existe"

echo "Construyendo imágenes Docker..."

# Construir imágenes
docker build -t crediya-dev-autenticacion ./autenticacion -f ./autenticacion/deployment/Dockerfile --build-arg STAGE=dev
docker build -t crediya-dev-solicitudes ./solicitudes -f ./solicitudes/deployment/Dockerfile --build-arg STAGE=dev
docker build -t crediya-dev-reportes ./reportes -f ./reportes/deployment/Dockerfile --build-arg STAGE=dev

echo "Autenticando con ECR..."
aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com

echo "Tageando imágenes para ECR..."
docker tag crediya-dev-autenticacion:latest $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/crediya-dev-autenticacion:latest
docker tag crediya-dev-solicitudes:latest $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/crediya-dev-solicitudes:latest
docker tag crediya-dev-reportes:latest $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/crediya-dev-reportes:latest

echo "Subiendo imágenes a ECR..."
docker push $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/crediya-dev-autenticacion:latest
docker push $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/crediya-dev-solicitudes:latest
docker push $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/crediya-dev-reportes:latest

echo "Imágenes subidas exitosamente!"
echo "Ahora puedes ejecutar: serverless deploy"