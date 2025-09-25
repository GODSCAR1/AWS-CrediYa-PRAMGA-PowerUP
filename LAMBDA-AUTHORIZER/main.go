package main

import (
	"context"
	"encoding/base64"
	"fmt"
	"io"
	"log"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/secretsmanager"
	"github.com/golang-jwt/jwt/v5"
)

type JWTClaims struct {
	Email string `json:"email"`
	Role  string `json:"role"`
	jwt.RegisteredClaims
}

type SecretsManagerClient struct {
	client *secretsmanager.Client
}

var (
	cachedSecret  string
	secretsClient *SecretsManagerClient
)

func NewSecretsManagerClient(ctx context.Context) (*SecretsManagerClient, error) {
	cfg, err := config.LoadDefaultConfig(ctx)
	if err != nil {
		return nil, fmt.Errorf("failed to load AWS config: %w", err)
	}

	return &SecretsManagerClient{
		client: secretsmanager.NewFromConfig(cfg),
	}, nil
}

func (s *SecretsManagerClient) GetSecret(ctx context.Context) (string, error) {
	if cachedSecret != "" {
		return cachedSecret, nil
	}

	stage := os.Getenv("STAGE")
	if stage == "" {
		return "", fmt.Errorf("STAGE environment variable not set")
	}

	secretName := fmt.Sprintf("crediya-%s-jwt-secret-key", stage)
	input := &secretsmanager.GetSecretValueInput{
		SecretId: aws.String(secretName),
	}

	result, err := s.client.GetSecretValue(ctx, input)
	if err != nil {
		return "", fmt.Errorf("failed to get secret %s: %w", secretName, err)
	}

	if result.SecretString == nil {
		return "", fmt.Errorf("secret %s has no string value", secretName)
	}

	cachedSecret = *result.SecretString
	return cachedSecret, nil
}

func main() {
	lambda.Start(handler)
}

func handler(ctx context.Context, event events.APIGatewayV2HTTPRequest) (events.APIGatewayV2HTTPResponse, error) {
	log.Printf("Request: %s %s", event.RequestContext.HTTP.Method, event.RawPath)

	// 1. Obtener ALB_URL de variables de entorno (REQUERIDO)
	albURL := os.Getenv("ALB_URL")
	if albURL == "" {
		log.Println("ERROR: ALB_URL environment variable not set")
		return events.APIGatewayV2HTTPResponse{
			StatusCode: 500,
			Body:       `{"error": "Server configuration error"}`,
		}, nil
	}

	// 2. Construir URL final para el ALB
	finalURL := buildFinalURL(albURL, event)

	// 3. Preparar headers base para forward
	forwardHeaders := prepareForwardHeaders(event.Headers)

	// 4. Procesar token JWT (OPCIONAL)
	token := extractToken(event.Headers)
	if token != "" {
		log.Println("Token found, attempting validation")
		processJWTToken(ctx, token, forwardHeaders)
	} else {
		log.Println("No token provided, forwarding without authentication headers")
	}

	// 5. Procesar body de la request
	requestBody, err := processRequestBody(event)
	if err != nil {
		log.Printf("Error processing request body: %v", err)
		return events.APIGatewayV2HTTPResponse{
			StatusCode: 400,
			Body:       `{"error": "Invalid request body"}`,
		}, nil
	}

	// 6. Hacer forward al ALB
	log.Printf("Forwarding to: %s", finalURL)
	response, err := makeRequest(ctx, event.RequestContext.HTTP.Method, finalURL, requestBody, forwardHeaders)
	if err != nil {
		log.Printf("Error making request to ALB: %v", err)
		return events.APIGatewayV2HTTPResponse{
			StatusCode: 502,
			Body:       `{"error": "Bad Gateway"}`,
		}, nil
	}

	return response, nil
}

// buildFinalURL construye la URL final para el ALB
func buildFinalURL(albURL string, event events.APIGatewayV2HTTPRequest) string {
	// Remover el stage del path usando la variable de entorno
	path := event.RawPath
	stage := os.Getenv("STAGE")

	if stage != "" {
		stagePrefix := "/" + stage
		// Si el path empieza con el stage, lo removemos
		if strings.HasPrefix(path, stagePrefix) {
			path = strings.TrimPrefix(path, stagePrefix)
		}
	}

	// Si el path queda vacío, usar "/"
	if path == "" {
		path = "/"
	}

	finalURL := albURL + path

	if event.RawQueryString != "" {
		finalURL += "?" + event.RawQueryString
	} else if len(event.QueryStringParameters) > 0 {
		params := url.Values{}
		for k, v := range event.QueryStringParameters {
			params.Add(k, v)
		}
		if len(params) > 0 {
			finalURL += "?" + params.Encode()
		}
	}

	return finalURL
}

// extractToken extrae el token de los headers (opcional)
func extractToken(headers map[string]string) string {
	if auth, exists := headers["authorization"]; exists {
		return strings.TrimPrefix(auth, "Bearer ")
	}
	if auth, exists := headers["Authorization"]; exists {
		return strings.TrimPrefix(auth, "Bearer ")
	}
	return ""
}

// prepareForwardHeaders prepara los headers para hacer forward
func prepareForwardHeaders(originalHeaders map[string]string) map[string]string {
	forwardHeaders := make(map[string]string)

	// Copiar todos los headers originales
	for k, v := range originalHeaders {
		forwardHeaders[k] = v
	}

	// Eliminar headers problemáticos que pueden causar conflictos
	problematicHeaders := []string{
		"host", "Host",
		"content-length", "Content-Length",
		"transfer-encoding", "Transfer-Encoding",
		"connection", "Connection",
		"te", "TE",
		"upgrade", "Upgrade",
		"proxy-authenticate", "Proxy-Authenticate",
		"proxy-authorization", "Proxy-Authorization",
		"trailer", "Trailer",
	}

	for _, header := range problematicHeaders {
		delete(forwardHeaders, header)
	}

	return forwardHeaders
}

// processJWTToken procesa y valida el token JWT
func processJWTToken(ctx context.Context, token string, forwardHeaders map[string]string) {
	// Inicializar cliente de secrets si no existe
	if secretsClient == nil {
		var err error
		secretsClient, err = NewSecretsManagerClient(ctx)
		if err != nil {
			log.Printf("Warning: Could not create secrets client: %v", err)
			return
		}
	}

	// Obtener JWT secret
	jwtSecret, err := secretsClient.GetSecret(ctx)
	if err != nil {
		log.Printf("Warning: Could not get JWT secret: %v", err)
		return
	}

	// Validar token
	claims, err := validateJWTToken(token, jwtSecret)
	if err != nil {
		log.Printf("Warning: JWT validation failed: %v", err)
		return
	}

	log.Printf("Token validated for user: %s, role: %s", claims.Email, claims.Role)

	// Agregar headers de usuario SOLO si la validación fue exitosa
	forwardHeaders["X-User-Email"] = claims.Email
	forwardHeaders["X-User-Role"] = claims.Role
}

// validateJWTToken valida el token JWT siguiendo la lógica de Java
func validateJWTToken(tokenString, secret string) (*JWTClaims, error) {
	// Decodificar el secret de base64 (igual que en Java)
	secretBytes, err := base64.StdEncoding.DecodeString(secret)
	if err != nil {
		return nil, fmt.Errorf("failed to decode base64 secret: %w", err)
	}

	// Parsear el token
	token, err := jwt.ParseWithClaims(tokenString, &JWTClaims{}, func(token *jwt.Token) (interface{}, error) {
		// Verificar que el método de signing sea HMAC (igual que en Java)
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return secretBytes, nil
	})

	if err != nil {
		return nil, fmt.Errorf("failed to parse token: %w", err)
	}

	claims, ok := token.Claims.(*JWTClaims)
	if !ok || !token.Valid {
		return nil, fmt.Errorf("invalid token claims")
	}

	// Validar campos requeridos
	if claims.Email == "" {
		return nil, fmt.Errorf("email claim is missing")
	}
	if claims.Role == "" {
		return nil, fmt.Errorf("role claim is missing")
	}

	return claims, nil
}

// processRequestBody procesa el body de la request
func processRequestBody(event events.APIGatewayV2HTTPRequest) (string, error) {
	if event.Body == "" {
		return "", nil
	}

	if event.IsBase64Encoded {
		decodedBytes, err := base64.StdEncoding.DecodeString(event.Body)
		if err != nil {
			return "", fmt.Errorf("failed to decode base64 body: %w", err)
		}
		return string(decodedBytes), nil
	}

	return event.Body, nil
}

// makeRequest hace la request HTTP al ALB
func makeRequest(ctx context.Context, method, requestURL, body string, headers map[string]string) (events.APIGatewayV2HTTPResponse, error) {
	client := &http.Client{
		Timeout: 30 * time.Second, // Timeout generoso para requests lentas
	}

	var reqBody io.Reader
	if body != "" {
		reqBody = strings.NewReader(body)
	}

	req, err := http.NewRequestWithContext(ctx, method, requestURL, reqBody)
	if err != nil {
		return events.APIGatewayV2HTTPResponse{}, fmt.Errorf("failed to create request: %w", err)
	}

	// Agregar headers
	for k, v := range headers {
		req.Header.Set(k, v)
	}

	// Asegurar Content-Type para requests con body
	if body != "" && req.Header.Get("Content-Type") == "" {
		req.Header.Set("Content-Type", "application/json")
	}

	// Hacer la request
	resp, err := client.Do(req)
	if err != nil {
		return events.APIGatewayV2HTTPResponse{}, fmt.Errorf("failed to make request: %w", err)
	}
	defer resp.Body.Close()

	// Leer response body
	responseBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return events.APIGatewayV2HTTPResponse{}, fmt.Errorf("failed to read response body: %w", err)
	}

	// Procesar response
	contentType := resp.Header.Get("Content-Type")
	var respBodyStr string
	var isBase64 bool

	// Determinar si es contenido texto o binario
	if isTextContent(contentType) {
		respBodyStr = string(responseBody)
		isBase64 = false
	} else {
		respBodyStr = base64.StdEncoding.EncodeToString(responseBody)
		isBase64 = true
	}

	// Convertir headers de response
	respHeaders := make(map[string]string)
	for k, v := range resp.Header {
		if len(v) > 0 {
			respHeaders[k] = v[0]
		}
	}

	log.Printf("Response: %d (body length: %d)", resp.StatusCode, len(respBodyStr))

	return events.APIGatewayV2HTTPResponse{
		StatusCode:      resp.StatusCode,
		Headers:         respHeaders,
		Body:            respBodyStr,
		IsBase64Encoded: isBase64,
	}, nil
}

// isTextContent determina si el contenido es texto
func isTextContent(contentType string) bool {
	textTypes := []string{
		"application/json",
		"application/xml",
		"application/javascript",
		"text/",
		"html",
	}

	contentType = strings.ToLower(contentType)
	for _, textType := range textTypes {
		if strings.Contains(contentType, textType) {
			return true
		}
	}
	return false
}
