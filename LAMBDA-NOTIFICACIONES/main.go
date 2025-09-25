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
)

// Estructura del mensaje que llega desde SQS
type SolicitudMessage struct {
	Monto       float64 `json:"monto"`
	Plazo       int     `json:"plazo"`
	TasaInteres float64 `json:"tasaInteres"`
	IdSolicitud string  `json:"idSolicitud"`
	Email       string  `json:"email"`
	Aprobado    bool    `json:"aprobado"`
}

type CuotaMensual struct {
	Mes            int     `json:"mes"`
	Capital        float64 `json:"capital"`
	Interes        float64 `json:"interes"`
	CuotaTotal     float64 `json:"cuota_total"`
	SaldoPendiente float64 `json:"saldo_pendiente"`
}

type PlanPagos struct {
	MontoTotal     float64        `json:"monto_total"`
	CuotaMensual   float64        `json:"cuota_mensual"`
	TotalIntereses float64        `json:"total_intereses"`
	Cuotas         []CuotaMensual `json:"cuotas"`
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
	asunto := "Préstamo Aprobado - Solicitud " + solicitud.IdSolicitud
	planPagos := generarPlanPagos(solicitud.Monto, solicitud.TasaInteres, solicitud.Plazo)
	cuerpo := generarTextoPlanPagos(planPagos, solicitud.IdSolicitud)

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
