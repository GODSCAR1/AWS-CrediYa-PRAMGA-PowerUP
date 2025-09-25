package co.com.crediya.sqs.sender;

import co.com.crediya.model.events.ReporteDiarioEvent;
import co.com.crediya.model.events.gateways.EventPublisherReporteDiario;
import co.com.crediya.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log
public class SQSSender implements EventPublisherReporteDiario {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper;

    public SQSSender(
            SQSSenderProperties properties,
            @Qualifier("senderSqsAsyncClient") SqsAsyncClient client,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = client;
        this.objectMapper = objectMapper;
    }
    @Override
    public void publishEventAsync(ReporteDiarioEvent event) {
        sendReporteDiarioEvent(event)
                .doOnSuccess(messageId -> log.info("Evento enviado: " + messageId +
                        " - Cantidad de prestamos aprobados: " + event.getCantidadPrestamosAprobados() +
                        " - Monto total aprobado: " + event.getCantidadTotalPrestada()
                ))
                .doOnError(error -> log.severe("Error enviando evento: " + error.getMessage()))
                .subscribe();
    }

    public Mono<String> sendReporteDiarioEvent(ReporteDiarioEvent event) {
        return Mono.fromCallable(() -> {
                    String messageBody = objectMapper.writeValueAsString(event);
                    return buildRequest(messageBody);
                })
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.info("Reporte enviado - MessageId: " + response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
