package co.com.crediya.consumer;

import co.com.crediya.consumer.exception.ExternalValidationException;
import co.com.crediya.model.solicitud.gateways.AutenticacionExternalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RestConsumer implements AutenticacionExternalService {
    private final WebClient client;

    @Override
    public Mono<Boolean> validateUsuario(String email) {
        return client.get()
                .uri("api/v1/usuario/{email}", email)
                .retrieve()
                .bodyToMono(RestConsumerResponse.class)
                .map(RestConsumerResponse::getFound)
                .onErrorResume( ex ->
                        Mono.error(new ExternalValidationException(ex.getMessage()))
                );
    }
}
