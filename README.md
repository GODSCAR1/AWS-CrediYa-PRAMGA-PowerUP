# CrediYa - Plataforma de Gestión de Préstamos

## Descripción General

CrediYa es una solución completa para la gestión de préstamos construida sobre una arquitectura de microservicios en AWS, utilizando Java 17 con programación reactiva. El sistema fue desarrollado utilizando el Plugin de Scaffold de Bancolombia, garantizando las mejores prácticas arquitectónicas y de desarrollo.

## Arquitectura de Microservicios

El sistema está compuesto por **3 microservicios principales**:

### 🔐 Microservicio de Autenticación
- Gestiona el registro, autenticación y autorización de usuarios
- Manejo de tokens JWT para sesiones seguras
- Validación de credenciales y permisos

### 💰 Microservicio de Solicitudes de Préstamo  
- Procesamiento de solicitudes de crédito
- Validación de datos del solicitante
- Integración con sistemas externos para verificación de información

### 📊 Microservicio de Reportes de Préstamos
- Generación de reportes administrativos
- Métricas y analíticas de préstamos
- Dashboards para administradores

## Arquitectura AWS

### Infraestructura Principal
La solución está desplegada completamente en AWS utilizando los siguientes servicios:

- **Amazon ECS**: Orquestación de contenedores para los microservicios con **Auto Scaling** automático
- **Application Load Balancer (ALB)**: Distribución inteligente de carga y enrutamiento entre microservicios
- **Amazon RDS**: Base de datos relacional principal para gestión de usuarios y solicitudes
- **Amazon DynamoDB**: Base de datos NoSQL optimizada para almacenamiento y consulta de reportes
- **Amazon ECR**: Registro privado de imágenes Docker con imágenes multi-stage optimizadas
- **Amazon VPC**: Red privada virtual con subredes públicas y privadas
- **AWS Secrets Manager**: Gestión segura de credenciales y configuraciones sensibles
- **Amazon CloudWatch**: Monitoreo completo, logging y **alarmas automáticas** para toda la infraestructura
- **AWS Auto Scaling**: Escalado automático de recursos basado en métricas de rendimiento

### Sistema de Comunicación Asíncrona

El sistema utiliza **Amazon SQS** como sistema de colas para la comunicación entre microservicios y las siguientes **AWS Lambda Functions**:

#### 🔔 Lambda de Notificaciones
- Procesa mensajes de la cola SQS
- Envía notificaciones por correo electrónico vía Amazon SES
- Gestiona diferentes tipos de notificaciones del sistema

#### 🧮 Lambda de Capacidad de Endeudamiento
- Calcula automáticamente la capacidad crediticia de los solicitantes
- **Aprueba o rechaza solicitudes de manera automática** basado en algoritmos predefinidos
- Notifica el resultado vía correo electrónico
- Actualiza el estado de las solicitudes en tiempo real

#### 📈 Lambda de Reportes Diarios
- Genera reportes automáticos diariamente
- Compila estadísticas y métricas del sistema
- Envía reportes por correo a los administradores vía Amazon SES

### Lambda-Proxy
La **Lambda-Proxy** actúa como un gateway inteligente que:
- Valida y extrae los headers del token JWT
- Autoriza las peticiones antes de enviarlas a los microservicios
- Enruta las peticiones al Application Load Balancer (ALB)
- Proporciona una capa adicional de seguridad

### Lambda DBINIT (Autodestructiva)
Esta lambda especial se ejecuta una única vez durante el despliegue inicial:
- Crea el usuario de base de datos necesario para los microservicios
- Inicializa los esquemas de base de datos
- Inserta datos iniciales requeridos
- Se autodestrue tras completar su función

## Tecnologías Utilizadas

- **Java 17** con programación reactiva (Spring WebFlux)
- **AWS Serverless Framework** para Infrastructure as Code
- **Docker** para containerización
- **Amazon SES** para envío de correos
- **Plugin Scaffold de Bancolombia** para estructura del proyecto
- **Serverless Go Plugin** para optimización de despliegues

## Instalación y Despliegue

### Prerrequisitos
1. **Serverless Framework**:
   ```bash
   npm install -g serverless
   ```

2. **Serverless Go Plugin**:
   ```bash
   npm install serverless-go-plugin
   ```

### Configuración Previa al Despliegue

**⚠️ IMPORTANTE - Configuración de Correos Electrónicos:**

Antes del despliegue, es necesario configurar los correos electrónicos para las notificaciones:

1. **Configurar emails en modo Sandbox de SES**:
   - Navega a la carpeta `coolestvariable/`
   - Edita el archivo `common.yml`
   - Agrega los correos electrónicos que recibirán las notificaciones del sistema
   - **Nota**: Amazon SES opera en modo Sandbox por defecto, por lo que solo los emails verificados y agregados en la configuración podrán recibir notificaciones.

### Proceso de Despliegue

Una vez configurados los prerrequisitos, permisos AWS y emails, el despliegue es extremadamente simple:

1. **Ejecutar el script de construcción y subida de imágenes**:
   ```bash
   ./deploy-images.sh
   ```
   Este script automatiza:
   - **Multi-Stage Build** de cada microservicio para generar los archivos `.jar` optimizados
   - Construcción de las imágenes Docker a partir de los `.jar` generados
   - Subida automática de todas las imágenes a **Amazon ECR**

2. **Desplegar la infraestructura completa**:
   ```bash
   serverless deploy
   ```

¡Y listo! El sistema completo se desplegará automáticamente en AWS con toda la infraestructura configurada.

### Permisos AWS Requeridos

Para ejecutar toda la pila, se necesita una cuenta AWS con los siguientes permisos:

- **CloudFormation**: Permisos completos para gestión de stacks
- **Lambda**: Permisos completos para creación y gestión de funciones
- **SQS**: Permisos completos para gestión de colas
- **SES**: Permisos completos para envío de correos
- **S3**: Permisos completos para almacenamiento
- **CloudWatch**: Permisos completos para logs y monitoreo
- **DynamoDB**: Permisos completos para tablas NoSQL
- **IAM**: Permisos específicos para creación y gestión de roles con prefijo 'crediya-*'
- **RDS**: Permisos para creación y gestión de instancias de base de datos
- **Secrets Manager**: Permisos completos para gestión de secretos
- **EC2**: Permisos completos para gestión de infraestructura de red
- **ECS**: Permisos completos para orquestación de contenedores
- **ECR**: Permisos para gestión de repositorios de imágenes Docker
- **EventBridge**: Permisos para gestión de eventos y reglas
- **STS**: Permisos para asumir roles
- **Elastic Load Balancing**: Permisos completos para balanceadores de carga
- **Application Auto Scaling**: Permisos para escalado automático
- **API Gateway**: Permisos completos para gestión de APIs
- **SNS**: Permisos completos para notificaciones

## Flujo de Trabajo del Sistema

1. **Autenticación**: Los usuarios se registran y autentican a través del microservicio de autenticación
2. **Solicitud de Préstamo**: Las solicitudes se procesan a través del microservicio correspondiente
3. **Evaluación Automática**: La lambda de capacidad de endeudamiento evalúa y decide automáticamente
4. **Notificación**: El sistema notifica al usuario el resultado vía correo electrónico
5. **Reportes**: Los administradores reciben reportes diarios automatizados

## Características Destacadas

- ✅ **Arquitectura Serverless** escalable y costo-eficiente
- ✅ **Multi-Stage Docker Build** para imágenes optimizadas y .jar compactos
- ✅ **Programación Reactiva** para alto rendimiento
- ✅ **Auto Scaling automático** basado en métricas de CPU y memoria
- ✅ **Evaluación Automática** de solicitudes de crédito
- ✅ **Base de datos híbrida**: RDS para transacciones y DynamoDB para reportes
- ✅ **Comunicación Asíncrona** mediante colas SQS
- ✅ **Seguridad Robusta** con JWT, Lambda-Proxy y Secrets Manager
- ✅ **Monitoreo Completo** con CloudWatch y alarmas automáticas
- ✅ **Balanceador de Carga** inteligente con ALB
- ✅ **Despliegue Automatizado** con Infrastructure as Code


*Nota: Al día de 25/09/2025 este reto sería mi primera vez utilizando AWS, CloudFormation y Serverless Framework, cualquier feedback es bien recibido.*
