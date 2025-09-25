package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/ses"
	"github.com/aws/aws-sdk-go-v2/service/ses/types"
)

// Estructura del reporte que recibe la lambda
type ReporteDiarioEvent struct {
	CantidadPrestamosAprobados int     `json:"cantidadPrestamosAprobados"`
	CantidadTotalPrestada      float64 `json:"cantidadTotalPrestada"`
}

// Cliente SES global
var sesClient *ses.Client

// Inicializar cliente SES
func init() {
	cfg, err := config.LoadDefaultConfig(context.TODO(), config.WithRegion(getEnvVar("AWS_REGION", "us-east-1")))
	if err != nil {
		log.Fatalf("Error creando configuración AWS: %v", err)
	}
	sesClient = ses.NewFromConfig(cfg)
}

// Función para obtener variables de entorno con valor por defecto
func getEnvVar(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

// Función principal de la lambda para manejar eventos SQS
func handleRequest(ctx context.Context, sqsEvent events.SQSEvent) error {
	log.Printf("Recibidos %d mensajes de SQS", len(sqsEvent.Records))

	// Obtener emails desde variables de entorno
	emailFrom := os.Getenv("EMAIL_FROM")
	emailTo := os.Getenv("EMAIL_TO")

	if emailFrom == "" || emailTo == "" {
		log.Printf("Error: Variables de entorno EMAIL_FROM y EMAIL_TO son requeridas")
		return fmt.Errorf("variables de entorno faltantes")
	}

	// Procesar cada mensaje en el batch
	for i, record := range sqsEvent.Records {
		log.Printf("Procesando mensaje %d de %d", i+1, len(sqsEvent.Records))
		log.Printf("Mensaje SQS recibido: %s", record.Body)

		// Parsear el contenido del mensaje
		var reporte ReporteDiarioEvent
		if err := json.Unmarshal([]byte(record.Body), &reporte); err != nil {
			log.Printf("Error parseando mensaje SQS: %v", err)
			continue // Continúa con el siguiente mensaje
		}

		// Log del reporte parseado
		reporteJSON, _ := json.Marshal(reporte)
		log.Printf("Reporte parseado: %s", string(reporteJSON))

		// Enviar email para este reporte
		if err := sendReportEmail(reporte, emailFrom, emailTo); err != nil {
			log.Printf("Error enviando email para mensaje %d: %v", i+1, err)
			return err // Si falla el envío, la lambda retorna error y SQS reintentará
		}

		log.Printf("Email enviado exitosamente para mensaje %d", i+1)
	}

	log.Printf("Procesamiento completado para todos los mensajes")
	return nil
}

// Función separada para enviar el email
func sendReportEmail(reporte ReporteDiarioEvent, emailFrom, emailTo string) error {
	// Crear contenido del email
	subject := "Reporte Diario de Préstamos Aprobados"
	htmlBody := fmt.Sprintf(`
		<html>
		<body>
			<h2>Reporte Diario de Préstamos</h2>
			<p><strong>Cantidad de préstamos aprobados:</strong> %d</p>
			<p><strong>Monto total aprobado:</strong> $%.2f</p>
			<hr>
			<p><small>Este es un email generado automáticamente por el sistema de reportes diarios.</small></p>
		</body>
		</html>
	`, reporte.CantidadPrestamosAprobados, reporte.CantidadTotalPrestada)

	textBody := fmt.Sprintf(`
Reporte Diario de Préstamos

Cantidad de préstamos aprobados: %d
Monto total aprobado: $%.2f

Este es un email generado automáticamente por el sistema de reportes diarios.
	`, reporte.CantidadPrestamosAprobados, reporte.CantidadTotalPrestada)

	// Preparar el email
	input := &ses.SendEmailInput{
		Destination: &types.Destination{
			ToAddresses: []string{emailTo},
		},
		Message: &types.Message{
			Body: &types.Body{
				Html: &types.Content{
					Charset: aws.String("UTF-8"),
					Data:    aws.String(htmlBody),
				},
				Text: &types.Content{
					Charset: aws.String("UTF-8"),
					Data:    aws.String(textBody),
				},
			},
			Subject: &types.Content{
				Charset: aws.String("UTF-8"),
				Data:    aws.String(subject),
			},
		},
		Source: aws.String(emailFrom),
	}

	// Enviar el email
	result, err := sesClient.SendEmail(context.TODO(), input)
	if err != nil {
		return fmt.Errorf("error enviando email: %v", err)
	}

	log.Printf("Email enviado exitosamente. MessageId: %s", *result.MessageId)
	return nil
}

func main() {
	lambda.Start(handleRequest)
}
