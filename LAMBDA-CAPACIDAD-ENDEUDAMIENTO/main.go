package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"math"
	"os"
	"strings"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-lambda-go/lambda"
	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/ses"
	"github.com/aws/aws-sdk-go-v2/service/ses/types"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
)

// Estructura de la solicitud de préstamo que llega por SQS
type SolicitudPrestamo struct {
	Email              string  `json:"email"`
	IdSolicitud        string  `json:"idSolicitud"`
	DeudaMensualActual float64 `json:"deudaMensualActual"`
	MontoSolicitado    float64 `json:"monto"`
	TasaInteres        float64 `json:"tasaInteres"` // Tasa mensual en porcentaje (ej: 1.25 para 1.25%)
	PlazoMeses         int     `json:"plazo"`
	Salario            float64 `json:"salario"`
}

// Estructura de la cuota mensual
type CuotaMensual struct {
	Mes            int     `json:"mes"`
	Capital        float64 `json:"capital"`
	Interes        float64 `json:"interes"`
	CuotaTotal     float64 `json:"cuota_total"`
	SaldoPendiente float64 `json:"saldo_pendiente"`
}

// Plan de pagos completo
type PlanPagos struct {
	MontoTotal     float64        `json:"monto_total"`
	CuotaMensual   float64        `json:"cuota_mensual"`
	TotalIntereses float64        `json:"total_intereses"`
	Cuotas         []CuotaMensual `json:"cuotas"`
}

// Resultado de la evaluación para enviar al microservicio
type MensajeMicroservicio struct {
	IdSolicitud string `json:"idSolicitud"`
	Estado      string `json:"estado"` // Aprobado, Rechazado, Pendiente de revision
	Email       string `json:"email"`
	Motivo      string `json:"motivo"`
}

// Resultado completo de la evaluación
type ResultadoEvaluacion struct {
	IdSolicitud                      string     `json:"idSolicitud"`
	Estado                           string     `json:"estado"`
	CapacidadEndeudamientoDisponible float64    `json:"capacidad_endeudamiento_disponible"`
	CuotaMensual                     float64    `json:"cuota_mensual"`
	Motivo                           string     `json:"motivo"`
	PlanPagos                        *PlanPagos `json:"plan_pagos,omitempty"`
}

var (
	sqsClient *sqs.Client
	sesClient *ses.Client
)

func init() {
	cfg, err := config.LoadDefaultConfig(context.TODO())
	if err != nil {
		log.Fatalf("Error loading AWS config: %v", err)
	}

	sqsClient = sqs.NewFromConfig(cfg)
	sesClient = ses.NewFromConfig(cfg)
}

// Calcula la cuota mensual usando la fórmula de amortización francesa
func calcularCuotaMensual(capital, tasaMensual float64, plazoMeses int) float64 {

	numerador := capital * tasaMensual * math.Pow(1+tasaMensual, float64(plazoMeses))
	denominador := math.Pow(1+tasaMensual, float64(plazoMeses)) - 1

	return numerador / denominador
}

// Genera el plan de pagos detallado
func generarPlanPagos(capital, tasaMensualPorcentual float64, plazoMeses int) *PlanPagos {
	tasaMensual := tasaMensualPorcentual / 100
	cuotaMensual := calcularCuotaMensual(capital, tasaMensual, plazoMeses)

	cuotas := make([]CuotaMensual, plazoMeses)
	saldoPendiente := capital
	totalIntereses := 0.0

	for i := 0; i < plazoMeses; i++ {
		pagoInteres := saldoPendiente * tasaMensual
		pagoCapital := cuotaMensual - pagoInteres

		if i == plazoMeses-1 {
			// Ajuste final para evitar errores de redondeo
			pagoCapital = saldoPendiente
			cuotaMensual = pagoCapital + pagoInteres
		}

		saldoPendiente -= pagoCapital
		totalIntereses += pagoInteres

		cuotas[i] = CuotaMensual{
			Mes:            i + 1,
			Capital:        math.Round(pagoCapital*100) / 100,
			Interes:        math.Round(pagoInteres*100) / 100,
			CuotaTotal:     math.Round(cuotaMensual*100) / 100,
			SaldoPendiente: math.Round(saldoPendiente*100) / 100,
		}
	}

	return &PlanPagos{
		MontoTotal:     capital,
		CuotaMensual:   math.Round(cuotaMensual*100) / 100,
		TotalIntereses: math.Round(totalIntereses*100) / 100,
		Cuotas:         cuotas,
	}
}

// Evalúa la solicitud de préstamo
func evaluarSolicitudPrestamo(ctx context.Context, solicitud *SolicitudPrestamo) (*ResultadoEvaluacion, error) {
	// Calcula la capacidad de endeudamiento disponible
	capacidadDisponible := solicitud.Salario*0.35 - solicitud.DeudaMensualActual

	// Calcula la cuota mensual del nuevo préstamo
	tasaMensual := solicitud.TasaInteres / 100
	cuotaMensual := calcularCuotaMensual(solicitud.MontoSolicitado, tasaMensual, solicitud.PlazoMeses)

	resultado := &ResultadoEvaluacion{
		IdSolicitud:                      solicitud.IdSolicitud,
		CapacidadEndeudamientoDisponible: capacidadDisponible,
		CuotaMensual:                     math.Round(cuotaMensual*100) / 100,
	}

	// Evalúa si la cuota cabe en la capacidad disponible
	if cuotaMensual > capacidadDisponible {
		resultado.Estado = "Rechazado"
		resultado.Motivo = fmt.Sprintf("La cuota mensual de $%.2f supera la capacidad de endeudamiento disponible de $%.2f", cuotaMensual, capacidadDisponible)
		return resultado, nil
	}

	// Verifica si el monto supera 5 veces el salario
	if solicitud.MontoSolicitado > solicitud.Salario*5 {
		resultado.Estado = "Pendiente de revision"
		resultado.Motivo = fmt.Sprintf("El monto solicitado de $%.2f supera 5 veces el salario de $%.2f. Requiere revisión manual.", solicitud.MontoSolicitado, solicitud.Salario)
		return resultado, nil
	}

	// Aprueba el préstamo y genera el plan de pagos
	resultado.Estado = "Aprobado"
	resultado.Motivo = "Solicitud aprobada automáticamente"
	resultado.PlanPagos = generarPlanPagos(solicitud.MontoSolicitado, solicitud.TasaInteres, solicitud.PlazoMeses)

	return resultado, nil
}

// Envía mensaje al microservicio vía SQS
func enviarMensajeMicroservicio(ctx context.Context, resultado *ResultadoEvaluacion, email string) error {
	urlColaMicroservicio := os.Getenv("MICROSERVICE_QUEUE_URL")
	if urlColaMicroservicio == "" {
		return fmt.Errorf("MICROSERVICE_QUEUE_URL environment variable not set")
	}

	mensaje := MensajeMicroservicio{
		IdSolicitud: resultado.IdSolicitud,
		Estado:      resultado.Estado,
		Email:       email,
		Motivo:      resultado.Motivo,
	}

	cuerpoMensaje, err := json.Marshal(mensaje)
	if err != nil {
		return fmt.Errorf("error marshaling microservice message: %v", err)
	}

	_, err = sqsClient.SendMessage(ctx, &sqs.SendMessageInput{
		QueueUrl:    aws.String(urlColaMicroservicio),
		MessageBody: aws.String(string(cuerpoMensaje)),
	})

	return err
}

// Genera el texto simple del plan de pagos para el email
func generarTextoPlanPagos(plan *PlanPagos, idSolicitud string) string {
	var texto strings.Builder

	texto.WriteString("¡FELICIDADES! TU PRÉSTAMO HA SIDO APROBADO\n")
	texto.WriteString("==========================================\n\n")
	texto.WriteString(fmt.Sprintf("Número de Solicitud: %s\n\n", idSolicitud))

	texto.WriteString("RESUMEN DEL PRÉSTAMO:\n")
	texto.WriteString(fmt.Sprintf("• Monto Total: $%.2f\n", plan.MontoTotal))
	texto.WriteString(fmt.Sprintf("• Cuota Mensual: $%.2f\n", plan.CuotaMensual))
	texto.WriteString(fmt.Sprintf("• Total de Intereses: $%.2f\n", plan.TotalIntereses))
	texto.WriteString(fmt.Sprintf("• Total a Pagar: $%.2f\n", plan.MontoTotal+plan.TotalIntereses))
	texto.WriteString(fmt.Sprintf("• Número de Cuotas: %d\n\n", len(plan.Cuotas)))

	texto.WriteString("PLAN DE PAGOS:\n")
	texto.WriteString("Mes | Capital   | Interés   | Cuota     | Saldo\n")
	texto.WriteString("----+-----------+-----------+-----------+-----------\n")

	for i, cuota := range plan.Cuotas {
		if i < 5 || i >= len(plan.Cuotas)-2 { // Muestra primeros 5 y últimos 2
			texto.WriteString(fmt.Sprintf("%3d | $%8.2f | $%8.2f | $%8.2f | $%8.2f\n",
				cuota.Mes, cuota.Capital, cuota.Interes, cuota.CuotaTotal, cuota.SaldoPendiente))
		} else if i == 5 {
			texto.WriteString("... | (cuotas intermedias omitidas) ...\n")
		}
	}

	texto.WriteString("\nPRÓXIMOS PASOS:\n")
	texto.WriteString("• En breve nos contactaremos contigo para finalizar el proceso\n")
	texto.WriteString("• Prepara la documentación requerida\n")
	texto.WriteString("• La primera cuota se cobrará 30 días después del desembolso\n\n")
	texto.WriteString("Gracias por confiar en nosotros.\n")
	texto.WriteString("Equipo de Créditos")

	return texto.String()
}

// Envía email de aprobación con plan de pagos
func enviarEmailAprobacion(ctx context.Context, resultado *ResultadoEvaluacion, email string) error {
	emailRemitente := os.Getenv("SES_FROM_EMAIL")
	if emailRemitente == "" {
		return fmt.Errorf("SES_FROM_EMAIL environment variable not set")
	}

	asunto := "Préstamo Aprobado - Solicitud " + resultado.IdSolicitud
	cuerpo := generarTextoPlanPagos(resultado.PlanPagos, resultado.IdSolicitud)

	_, err := sesClient.SendEmail(ctx, &ses.SendEmailInput{
		Source: aws.String(emailRemitente),
		Destination: &types.Destination{
			ToAddresses: []string{email},
		},
		Message: &types.Message{
			Subject: &types.Content{
				Data: aws.String(asunto),
			},
			Body: &types.Body{
				Text: &types.Content{
					Data: aws.String(cuerpo),
				},
			},
		},
	})

	return err
}

// Envía email de rechazo
func enviarEmailRechazo(ctx context.Context, resultado *ResultadoEvaluacion, email string, solicitud *SolicitudPrestamo) error {
	emailRemitente := os.Getenv("SES_FROM_EMAIL")
	if emailRemitente == "" {
		return fmt.Errorf("SES_FROM_EMAIL environment variable not set")
	}

	asunto := "Solicitud de Préstamo No Aprobada - " + resultado.IdSolicitud
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
		`, solicitud.IdSolicitud, solicitud.MontoSolicitado, solicitud.PlazoMeses)

	_, err := sesClient.SendEmail(ctx, &ses.SendEmailInput{
		Source: aws.String(emailRemitente),
		Destination: &types.Destination{
			ToAddresses: []string{email},
		},
		Message: &types.Message{
			Subject: &types.Content{
				Data: aws.String(asunto),
			},
			Body: &types.Body{
				Text: &types.Content{
					Data: aws.String(cuerpo),
				},
			},
		},
	})

	return err
}

// Procesa un mensaje individual de SQS
func procesarSolicitudPrestamo(ctx context.Context, mensajeSqs events.SQSMessage) error {
	log.Printf("Processing SQS message: %s", mensajeSqs.Body)

	// Deserializa la solicitud de préstamo
	var solicitud SolicitudPrestamo
	if err := json.Unmarshal([]byte(mensajeSqs.Body), &solicitud); err != nil {
		return fmt.Errorf("error unmarshaling loan request: %v", err)
	}

	// Valida los datos de entrada
	if solicitud.Email == "" || solicitud.IdSolicitud == "" {
		return fmt.Errorf("email and id_solicitud are required")
	}

	if solicitud.MontoSolicitado <= 0 || solicitud.Salario <= 0 || solicitud.PlazoMeses <= 0 {
		return fmt.Errorf("invalid numerical values in request")
	}

	// Evalúa la solicitud
	resultado, err := evaluarSolicitudPrestamo(ctx, &solicitud)
	if err != nil {
		return fmt.Errorf("error evaluating loan request: %v", err)
	}

	// Envía mensaje al microservicio (siempre se envía, independiente del estado)
	if err := enviarMensajeMicroservicio(ctx, resultado, solicitud.Email); err != nil {
		log.Printf("Error sending message to microservice: %v", err)
		return err
	}

	// Envía email solo para APROBADO y RECHAZADO
	if resultado.Estado == "Aprobado" {
		if err := enviarEmailAprobacion(ctx, resultado, solicitud.Email); err != nil {
			log.Printf("Error sending approval email: %v", err)
			return err
		}
	} else if resultado.Estado == "Rechazado" {
		if err := enviarEmailRechazo(ctx, resultado, solicitud.Email, &solicitud); err != nil {
			log.Printf("Error sending rejection email: %v", err)
			return err
		}
	}
	// Para POR_REVISION no se envía email automáticamente

	log.Printf("Loan evaluation completed successfully: IdSolicitud=%s, Estado=%s", resultado.IdSolicitud, resultado.Estado)
	return nil
}

// Handler principal de Lambda para procesar mensajes SQS
func handler(ctx context.Context, eventoSqs events.SQSEvent) error {
	log.Printf("Received %d messages from SQS", len(eventoSqs.Records))

	for _, mensaje := range eventoSqs.Records {
		if err := procesarSolicitudPrestamo(ctx, mensaje); err != nil {
			log.Printf("Error processing message %s: %v", mensaje.MessageId, err)
			// Retorna error para que el mensaje vuelva a la cola (DLQ si está configurado)
			return err
		}
	}

	return nil
}

func main() {
	lambda.Start(handler)
}
