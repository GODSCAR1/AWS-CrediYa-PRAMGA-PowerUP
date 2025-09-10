package co.com.crediya.sqs.sender;

import co.com.crediya.model.events.gateways.EventPublisher;
import co.com.crediya.model.events.SolicitudEvent;
import co.com.crediya.sqs.sender.config.SQSSenderProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@Log
@RequiredArgsConstructor
public class SQSSender implements EventPublisher {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper;


    @Override
    public void publishEventAsync(SolicitudEvent event) {
        sendSolicitudEvent(event)
                .doOnSuccess(messageId -> log.info("Evento enviado: " + messageId +
                        " - Solicitud: " + event.getIdSolicitud() +
                        " - Aceptado: " + event.getAprobado()))
                .doOnError(error -> log.severe("Error enviando evento: " + error.getMessage()))
                .subscribe();
    }

    public Mono<String> sendSolicitudEvent(SolicitudEvent event) {
        return Mono.fromCallable(() -> {
                        String messageBody = objectMapper.writeValueAsString(event);
                        return buildRequest(messageBody);
                })
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.info("Evento de solicitud enviado - ID: " +
                        event.getIdSolicitud() + ", MessageId: " + response.messageId()))
                .map(SendMessageResponse::messageId);
    }
    public Mono<String> send(String message) {
        return Mono.fromCallable(() -> buildRequest(message))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.info("Message sent: " + response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
