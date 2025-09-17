package co.com.crediya.sqs.sender;

import co.com.crediya.model.events.gateways.EventPublisherNotificaciones;
import co.com.crediya.model.events.SolicitudEventNotificaciones;
import co.com.crediya.sqs.sender.config.SQSSenderPropertiesNotifiaciones;
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
public class SQSSenderNotificaciones implements EventPublisherNotificaciones {
    private final SQSSenderPropertiesNotifiaciones properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper;

    public SQSSenderNotificaciones(
            SQSSenderPropertiesNotifiaciones properties,
            @Qualifier("senderSqsAsyncClientNotificaciones") SqsAsyncClient client,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishEventAsync(SolicitudEventNotificaciones event) {
        sendSolicitudEvent(event)
                .doOnSuccess(messageId -> log.info("Evento enviado: " + messageId +
                        " - Solicitud: " + event.getIdSolicitud() +
                        " - Aceptado: " + event.getAprobado()))
                .doOnError(error -> log.severe("Error enviando evento: " + error.getMessage()))
                .subscribe();
    }

    public Mono<String> sendSolicitudEvent(SolicitudEventNotificaciones event) {
        return Mono.fromCallable(() -> {
                        String messageBody = objectMapper.writeValueAsString(event);
                        return buildRequest(messageBody);
                })
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.info("Evento de solicitud enviado - ID: " +
                        event.getIdSolicitud() + ", MessageId: " + response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
