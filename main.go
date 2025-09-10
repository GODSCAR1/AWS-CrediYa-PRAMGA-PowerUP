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

// Estructura del mensaje que llega desde SQS
type SolicitudMessage struct {
	Monto       float64 `json:"monto"`
	Plazo       int     `json:"plazo"`
	IdSolicitud string  `json:"idSolicitud"`
	Email       string  `json:"email"`
	Aprobado    bool    `json:"aprobado"`
}

// Cliente SES global
var sesClient *ses.Client

func init() {
	cfg, err := config.LoadDefaultConfig(context.TODO())
	if err != nil {
		log.Fatalf("Error cargando configuración AWS: %v", err)
	}
	sesClient = ses.NewFromConfig(cfg)
}

func handler(ctx context.Context, sqsEvent events.SQSEvent) error {
	for _, record := range sqsEvent.Records {
		if err := procesarMensaje(ctx, record); err != nil {
			log.Printf("Error procesando mensaje: %v", err)
			return err // Falla y reintenta todo el batch
		}
	}
	return nil
}

func procesarMensaje(ctx context.Context, record events.SQSMessage) error {
	// Parsear mensaje JSON
	var solicitud SolicitudMessage
	if err := json.Unmarshal([]byte(record.Body), &solicitud); err != nil {
		return fmt.Errorf("error parseando JSON: %w", err)
	}

	// Enviar email según el resultado
	if solicitud.Aprobado {
		return enviarEmailAprobacion(ctx, solicitud)
	} else {
		return enviarEmailRechazo(ctx, solicitud)
	}
}

func enviarEmailAprobacion(ctx context.Context, solicitud SolicitudMessage) error {
	asunto := "¡Solicitud de Crédito Aprobada!"

	cuerpo := fmt.Sprintf(`
Estimado cliente,

¡Excelentes noticias! Su solicitud de crédito ha sido APROBADA.

Detalles de su solicitud:
- ID de Solicitud: %s
- Monto Aprobado: $%.2f
- Plazo: %d meses

Pronto nos pondremos en contacto con usted para continuar con el proceso.

Saludos cordiales,
Equipo de Créditos
`, solicitud.IdSolicitud, solicitud.Monto, solicitud.Plazo)

	return enviarEmail(ctx, solicitud.Email, asunto, cuerpo)
}

func enviarEmailRechazo(ctx context.Context, solicitud SolicitudMessage) error {
	asunto := "Resultado de su Solicitud de Crédito"

	cuerpo := fmt.Sprintf(`
		Estimado cliente,

		Lamentamos informarle que su solicitud de crédito no pudo ser aprobada en esta ocasión.

		Detalles de su solicitud:
		- ID de Solicitud: %s
		- Monto Solicitado: $%.2f
		- Plazo: %d meses

		Le invitamos a contactarnos para conocer más sobre nuestros productos alternativos.

		Saludos cordiales,
		Equipo de Créditos
		`, solicitud.IdSolicitud, solicitud.Monto, solicitud.Plazo)

	return enviarEmail(ctx, solicitud.Email, asunto, cuerpo)
}

func enviarEmail(ctx context.Context, destinatario, asunto, cuerpo string) error {
	// Email remitente desde variable de entorno
	remitente := os.Getenv("SES_FROM_EMAIL")
	if remitente == "" {
		return fmt.Errorf("variable SES_FROM_EMAIL no configurada")
	}

	input := &ses.SendEmailInput{
		Source: aws.String(remitente),
		Destination: &types.Destination{
			ToAddresses: []string{destinatario},
		},
		Message: &types.Message{
			Subject: &types.Content{
				Data:    aws.String(asunto),
				Charset: aws.String("UTF-8"),
			},
			Body: &types.Body{
				Text: &types.Content{
					Data:    aws.String(cuerpo),
					Charset: aws.String("UTF-8"),
				},
			},
		},
	}

	result, err := sesClient.SendEmail(ctx, input)
	if err != nil {
		return fmt.Errorf("error enviando email: %w", err)
	}

	log.Printf("Email enviado a %s. MessageID: %s", destinatario, *result.MessageId)
	return nil
}

func main() {
	lambda.Start(handler)
}
