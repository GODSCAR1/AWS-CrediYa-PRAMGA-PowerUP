package co.com.crediya.sqs.sender;

import co.com.crediya.model.events.SolicitudEventCapacidadEndeudamiento;
import co.com.crediya.model.events.gateways.EventPublisherCapacidadEndeudamiento;
import co.com.crediya.sqs.sender.config.SQSSenderPropertiesCapacidadEndeudamiento;
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
public class SQSSenderCapacidadEndeudamiento implements EventPublisherCapacidadEndeudamiento {
    private final SQSSenderPropertiesCapacidadEndeudamiento properties;
    private final SqsAsyncClient client;
    private final ObjectMapper objectMapper;

    public SQSSenderCapacidadEndeudamiento(
            SQSSenderPropertiesCapacidadEndeudamiento properties,
            @Qualifier("senderSqsAsyncClientCapacidadEndeudamiento") SqsAsyncClient client,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = client;
        this.objectMapper = objectMapper;
    }
    @Override
    public void publishEventAsync(SolicitudEventCapacidadEndeudamiento event) {
        sendSolicitudEvent(event)
                .doOnSuccess(messageId -> log.info("Evento enviado: " + messageId +
                        " - Solicitud: " + event.getIdSolicitud()))
                .doOnError(error -> log.severe("Error enviando evento: " + error.getMessage()))
                .subscribe();
    }

    public Mono<String> sendSolicitudEvent(SolicitudEventCapacidadEndeudamiento event) {
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
