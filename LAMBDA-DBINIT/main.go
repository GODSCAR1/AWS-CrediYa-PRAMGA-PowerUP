package main

import (
	"bytes"
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"
	"time"

	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	lambdaService "github.com/aws/aws-sdk-go-v2/service/lambda"
	"github.com/aws/aws-sdk-go-v2/service/secretsmanager"
	_ "github.com/go-sql-driver/mysql"
)

// SQL para la base de datos de autenticación
const autenticacionSQL = `
CREATE DATABASE IF NOT EXISTS autenticacion
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE autenticacion;

CREATE TABLE IF NOT EXISTS rol(
    id_rol      CHAR(36) DEFAULT (UUID()),
    nombre      VARCHAR(120) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_rol)
);

CREATE TABLE IF NOT EXISTS usuario(
    id_usuario          CHAR(36) DEFAULT (UUID()),
    nombre              VARCHAR(120) NOT NULL,
    apellido            VARCHAR(120) NOT NULL,
    email               VARCHAR(160) NOT NULL UNIQUE,
    contrasena          VARCHAR(255) NOT NULL,
    documento_identidad VARCHAR(15) NOT NULL UNIQUE,
    telefono            VARCHAR(15),
    id_rol              CHAR(36) NOT NULL,
    salario_base        DECIMAL(15,2) NOT NULL,
    fecha_nacimiento    DATE,
    PRIMARY KEY (id_usuario),
    CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'),
    CHECK (documento_identidad REGEXP '^[0-9]+$'),
    CHECK (salario_base >= 0),
    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
);

INSERT INTO rol(nombre, descripcion)    
VALUES
    ('ADMIN', 'Administrador del sistema.'),
    ('CLIENTE', 'Cliente del comercio'),
    ('ASESOR', 'Asesor comercial.');

INSERT INTO usuario(nombre, apellido, email, contrasena, documento_identidad, telefono, id_rol, salario_base, fecha_nacimiento)
SELECT 'Admin', 'Sistema', 'admin@empresa.com', '$2a$12$UsQQsZNa7disrj8IvO0Wk.KXgANifFlW8SoyHSi1UkKE.9xDVJjXS', '12345678', '3001234567', r.id_rol, 5000000.00, '1980-01-01'
FROM rol r 
WHERE r.nombre = 'ADMIN' 
AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email = 'admin@empresa.com');

INSERT INTO usuario(nombre, apellido, email, contrasena, documento_identidad, telefono, id_rol, salario_base, fecha_nacimiento)
SELECT 'Asesor', 'Sistema', 'asesor@empresa.com', '$2a$12$UsQQsZNa7disrj8IvO0Wk.KXgANifFlW8SoyHSi1UkKE.9xDVJjXS', '12345679', '3001234568', r.id_rol, 5000000.00, '1980-01-02'
FROM rol r 
WHERE r.nombre = 'ASESOR' 
AND NOT EXISTS (SELECT 1 FROM usuario u WHERE u.email = 'asesor@empresa.com');
`

// SQL para la base de datos de solicitudes
const solicitudesSQL = `
CREATE DATABASE IF NOT EXISTS solicitudes
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE solicitudes;

CREATE TABLE IF NOT EXISTS estados(
    id_estado   CHAR(36) DEFAULT (UUID()),
    nombre      VARCHAR(120) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    PRIMARY KEY (id_estado)
);

CREATE TABLE IF NOT EXISTS tipo_prestamo(
    id_tipo_prestamo        CHAR(36) DEFAULT (UUID()),
    nombre                  VARCHAR(120) NOT NULL UNIQUE,
    monto_minimo            DECIMAL(15,2) NOT NULL,
    monto_maximo            DECIMAL(15,2) NOT NULL,
    tasa_interes            DECIMAL(5,2) NOT NULL,
    validacion_automatica   BOOLEAN NOT NULL, 
    PRIMARY KEY (id_tipo_prestamo),
    CHECK(monto_minimo > 0),
    CHECK(monto_maximo > 0),
    CHECK(monto_maximo > monto_minimo),
    CHECK(tasa_interes > 0)
);

CREATE TABLE IF NOT EXISTS solicitud(
    id_solicitud        CHAR(36) DEFAULT (UUID()),
    monto               DECIMAL(15,2),
    plazo               INT NOT NULL,
    email               VARCHAR(160),
    id_estado           CHAR(36) NOT NULL,
    id_tipo_prestamo    CHAR(36) NOT NULL,
    PRIMARY KEY (id_solicitud),
    CHECK (monto > 0),
    CHECK (plazo > 0),
    CHECK (email REGEXP '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$'),
    CONSTRAINT fk_solicitud_estado
        FOREIGN KEY (id_estado) REFERENCES estados(id_estado),
    CONSTRAINT fk_solicitud_tipo_prestamo
        FOREIGN KEY (id_tipo_prestamo) REFERENCES tipo_prestamo(id_tipo_prestamo)
);

INSERT IGNORE INTO estados (nombre, descripcion) VALUES
('Pendiente de revision', 'Solicitud creada y pendiente de validaciones iniciales'),
('Rechazado', 'Solicitud no aprobada segun reglas automaticas o analisis'),
('Aprobado', 'Solicitud aprobada puede continuar a desembolso');

INSERT INTO tipo_prestamo (nombre, monto_minimo, monto_maximo, tasa_interes, validacion_automatica) 
SELECT 'Microcredito', 200000.00, 3000000.00, 2.10, TRUE
WHERE NOT EXISTS (SELECT 1 FROM tipo_prestamo WHERE nombre = 'Microcredito');

INSERT INTO tipo_prestamo (nombre, monto_minimo, monto_maximo, tasa_interes, validacion_automatica) 
SELECT 'Consumo', 1000000.00, 50000000.00, 1.60, TRUE
WHERE NOT EXISTS (SELECT 1 FROM tipo_prestamo WHERE nombre = 'Consumo');

INSERT INTO tipo_prestamo (nombre, monto_minimo, monto_maximo, tasa_interes, validacion_automatica) 
SELECT 'Hipotecario', 30000000.00, 500000000.00, 0.95, FALSE
WHERE NOT EXISTS (SELECT 1 FROM tipo_prestamo WHERE nombre = 'Hipotecario');
`

type SecretValue struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type LambdaClient struct {
	client *lambdaService.Client
}

func (l *LambdaClient) DeleteFunction(ctx context.Context, functionName string) error {
	input := &lambdaService.DeleteFunctionInput{
		FunctionName: aws.String(functionName),
	}

	_, err := l.client.DeleteFunction(ctx, input)
	return err
}

func getMasterCredentials(ctx context.Context) (string, string, error) {
	cfg, err := config.LoadDefaultConfig(ctx)
	if err != nil {
		return "", "", fmt.Errorf("error loading AWS config: %v", err)
	}

	secretsClient := secretsmanager.NewFromConfig(cfg)
	secretName := os.Getenv("DB_MASTER_SECRET")

	result, err := secretsClient.GetSecretValue(ctx, &secretsmanager.GetSecretValueInput{
		SecretId: aws.String(secretName),
	})
	if err != nil {
		return "", "", fmt.Errorf("error getting master secret: %v", err)
	}

	var secret SecretValue
	if err := json.Unmarshal([]byte(*result.SecretString), &secret); err != nil {
		return "", "", fmt.Errorf("error parsing master secret: %v", err)
	}

	return secret.Username, secret.Password, nil
}

func createApplicationUser(db *sql.DB, appUsername, appPassword string) error {
	log.Printf("Creando usuario de aplicación: %s", appUsername)

	// 1. Verificar si el usuario ya existe
	var userExists int
	checkUserSQL := fmt.Sprintf("SELECT COUNT(*) FROM mysql.user WHERE User = '%s' AND Host = '%%'", appUsername)
	err := db.QueryRow(checkUserSQL).Scan(&userExists)
	if err != nil {
		return fmt.Errorf("error verificando si usuario existe: %v", err)
	}

	if userExists > 0 {
		log.Printf("Usuario %s ya existe, actualizando contraseña...", appUsername)
		// Actualizar contraseña existente
		alterUserSQL := fmt.Sprintf("ALTER USER '%s'@'%%' IDENTIFIED BY '%s'", appUsername, appPassword)
		_, err = db.Exec(alterUserSQL)
		if err != nil {
			return fmt.Errorf("error actualizando contraseña de usuario %s: %v", appUsername, err)
		}
	} else {
		log.Printf("Creando nuevo usuario %s...", appUsername)
		// Crear el usuario
		createUserSQL := fmt.Sprintf("CREATE USER '%s'@'%%' IDENTIFIED BY '%s'", appUsername, appPassword)
		_, err = db.Exec(createUserSQL)
		if err != nil {
			return fmt.Errorf("error creando usuario %s: %v", appUsername, err)
		}
	}

	// 2. Otorgar permisos completos en las bases de datos de aplicación
	grants := []string{
		fmt.Sprintf("GRANT ALL PRIVILEGES ON autenticacion.* TO '%s'@'%%'", appUsername),
		fmt.Sprintf("GRANT ALL PRIVILEGES ON solicitudes.* TO '%s'@'%%'", appUsername),
		"FLUSH PRIVILEGES",
	}

	for _, grantSQL := range grants {
		_, err := db.Exec(grantSQL)
		if err != nil {
			return fmt.Errorf("error otorgando permisos con '%s': %v", grantSQL, err)
		}
	}

	log.Printf("Usuario de aplicación %s configurado con permisos completos", appUsername)
	return nil
}

func executeSQL(db *sql.DB, sqlScript, description string) error {
	log.Printf("Ejecutando: %s", description)

	// Dividir el SQL en statements individuales
	statements := strings.Split(sqlScript, ";")

	for _, statement := range statements {
		trimmed := strings.TrimSpace(statement)
		if trimmed == "" {
			continue
		}

		log.Printf("Ejecutando statement: %.100s...", trimmed)

		_, err := db.Exec(trimmed)
		if err != nil {
			return fmt.Errorf("error ejecutando statement '%s': %v", trimmed[:min(50, len(trimmed))], err)
		}
	}

	log.Printf("%s completado exitosamente", description)
	return nil
}

func selfDestruct(ctx context.Context) error {
	log.Println("Iniciando autodestrucción de la función Lambda...")

	cfg, err := config.LoadDefaultConfig(ctx)
	if err != nil {
		return fmt.Errorf("error loading AWS config: %v", err)
	}

	lambdaClient := &LambdaClient{
		client: lambdaService.NewFromConfig(cfg),
	}

	functionName := os.Getenv("AWS_LAMBDA_FUNCTION_NAME")

	if err := lambdaClient.DeleteFunction(ctx, functionName); err != nil {
		return fmt.Errorf("error deleting lambda function: %v", err)
	}

	log.Println("Función Lambda eliminada exitosamente")
	return nil
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}

func sendCFResponse(ctx context.Context, event map[string]interface{}, status string, reason string, data map[string]interface{}) error {
	responseBody := map[string]interface{}{
		"Status":             status,
		"Reason":             reason,
		"PhysicalResourceId": event["LogicalResourceId"],
		"StackId":            event["StackId"],
		"RequestId":          event["RequestId"],
		"LogicalResourceId":  event["LogicalResourceId"],
		"Data":               data,
	}

	jsonResponse, err := json.Marshal(responseBody)
	if err != nil {
		log.Printf("Error marshaling response: %v", err)
		return err
	}

	responseURL, ok := event["ResponseURL"].(string)
	if !ok {
		log.Printf("Error: ResponseURL not found in event")
		return fmt.Errorf("ResponseURL not found")
	}

	log.Printf("Sending response to CloudFormation: %s", string(jsonResponse))
	log.Printf("Response URL: %s", responseURL)

	// Enviar la respuesta HTTP a CloudFormation
	req, err := http.NewRequestWithContext(ctx, "PUT", responseURL, bytes.NewReader(jsonResponse))
	if err != nil {
		log.Printf("Error creating request: %v", err)
		return err
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Content-Length", fmt.Sprintf("%d", len(jsonResponse)))

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		log.Printf("Error sending response: %v", err)
		return err
	}
	defer resp.Body.Close()

	log.Printf("Response sent successfully. HTTP Status: %d", resp.StatusCode)

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("CloudFormation returned non-success status: %d", resp.StatusCode)
	}

	return nil
}

func handler(ctx context.Context, event map[string]interface{}) error {
	log.Println("=== INICIANDO LAMBDA VIA CUSTOM RESOURCE ===")
	log.Printf("Event: %+v", event)

	// Solo procesar en CREATE
	requestType, ok := event["RequestType"].(string)
	if !ok || requestType != "Create" {
		log.Printf("Event type '%s' - enviando respuesta exitosa", requestType)
		return sendCFResponse(ctx, event, "SUCCESS", "No action needed for "+requestType, nil)
	}

	log.Println("Custom Resource CREATE - Procediendo con inicialización...")

	// Obtener las credenciales maestras de RDS
	masterUsername, masterPassword, err := getMasterCredentials(ctx)
	if err != nil {
		log.Printf("Error obteniendo credenciales maestras: %v", err)
		sendCFResponse(ctx, event, "FAILED", err.Error(), nil)
		return err
	}

	// Obtener credenciales de aplicación desde variables de entorno
	appUsername := os.Getenv("APP_USERNAME")
	appPassword := os.Getenv("APP_PASSWORD")

	if appUsername == "" || appPassword == "" {
		err := fmt.Errorf("credenciales de aplicación no configuradas")
		log.Printf("Error: %v", err)
		sendCFResponse(ctx, event, "FAILED", err.Error(), nil)
		return err
	}

	// Configurar la conexión a la base de datos
	dbHost := os.Getenv("DB_HOST")
	dbPort := os.Getenv("DB_PORT")

	dsn := fmt.Sprintf("%s:%s@tcp(%s:%s)/", masterUsername, masterPassword, dbHost, dbPort)

	log.Println("Conectando a la base de datos...")

	db, err := sql.Open("mysql", dsn)
	if err != nil {
		log.Printf("Error creando conexión: %v", err)
		sendCFResponse(ctx, event, "FAILED", err.Error(), nil)
		return err
	}
	defer db.Close()

	// Configurar conexión
	db.SetConnMaxLifetime(time.Minute * 5)
	db.SetMaxOpenConns(2)
	db.SetMaxIdleConns(1)

	// Probar la conexión
	err = db.PingContext(ctx)
	if err != nil {
		log.Printf("Error conectando a la base de datos: %v", err)
		sendCFResponse(ctx, event, "FAILED", err.Error(), nil)
		return err
	}

	log.Println("Conexión establecida exitosamente")

	// 1. Ejecutar SQL para base de datos de autenticación
	if err := executeSQL(db, autenticacionSQL, "Creación de base de datos de autenticación"); err != nil {
		log.Printf("Error en autenticación: %v", err)
		sendCFResponse(ctx, event, "FAILED", err.Error(), nil)
		return err
	}

	// 2. Ejecutar SQL para base de datos de solicitudes
	if err := executeSQL(db, solicitudesSQL, "Creación de base de datos de solicitudes"); err != nil {
		log.Printf("Error en solicitudes: %v", err)
		sendCFResponse(ctx, event, "FAILED", err.Error(), nil)
		return err
	}

	// 3. Crear usuario de aplicación para microservicios
	if err := createApplicationUser(db, appUsername, appPassword); err != nil {
		log.Printf("Error creando usuario de aplicación: %v", err)
		sendCFResponse(ctx, event, "FAILED", err.Error(), nil)
		return err
	}

	log.Println("Inicialización completa de base de datos exitosa!")
	log.Printf("Resumen:")
	log.Printf("   Base de datos 'autenticacion' creada con tablas y datos")
	log.Printf("   Base de datos 'solicitudes' creada con tablas y datos")
	log.Printf("   Usuario '%s' creado para microservicios", appUsername)
	log.Printf("   Permisos otorgados en ambas bases de datos")

	// Enviar respuesta exitosa ANTES de autodestruirse
	responseData := map[string]interface{}{
		"Message":          "Inicialización completa exitosa - RDS listo para microservicios",
		"DatabasesCreated": []string{"autenticacion", "solicitudes"},
		"AppUserCreated":   appUsername,
		"Timestamp":        time.Now().Format(time.RFC3339),
	}

	if err := sendCFResponse(ctx, event, "SUCCESS", "Database initialization completed", responseData); err != nil {
		log.Printf("Error enviando respuesta: %v", err)
		return err
	}

	// Pequeño delay para asegurar que la respuesta se envió
	time.Sleep(2 * time.Second)

	// Autodestruir la función Lambda
	if err := selfDestruct(context.Background()); err != nil {
		log.Printf("Error durante autodestrucción (no crítico): %v", err)
	}

	return nil
}

func main() {
	lambda.Start(handler)
}
