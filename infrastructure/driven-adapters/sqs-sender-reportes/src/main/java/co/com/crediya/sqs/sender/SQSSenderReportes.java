package co.com.crediya.sqs.sender;

import co.com.crediya.model.events.SolicitudEventNotificaciones;
import co.com.crediya.model.events.SolicitudEventReportes;
import co.com.crediya.model.events.gateways.EventPublisherReportes;
import co.com.crediya.sqs.sender.config.SQSSenderPropertiesReportes;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log
public class SQSSenderReportes implements EventPublisherReportes {
    private final SQSSenderPropertiesReportes properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper;

    public SQSSenderReportes(
            SQSSenderPropertiesReportes properties,
            @Qualifier("senderSqsAsyncClientReportes") SqsAsyncClient client,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishEventAsync(SolicitudEventReportes event) {
        sendSolicitudEvent(event)
                .doOnSuccess(messageId -> log.info("Evento enviado: " + messageId +
                        " - Monto: " + event.getMonto()))
                .doOnError(error -> log.severe("Error enviando evento: " + error.getMessage()))
                .subscribe();
    }

    public Mono<String> sendSolicitudEvent(SolicitudEventReportes event) {
        return Mono.fromCallable(() -> {
                    String messageBody = objectMapper.writeValueAsString(event);
                    return buildRequest(messageBody);
                })
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.info("Evento de solicitud enviado - MessageId: " + response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
