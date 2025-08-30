package co.com.crediya.api;

import co.com.crediya.api.dto.CreateSolicitudRequest;
import co.com.crediya.api.dto.CreateSolicitudResponse;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.usecase.SolicitudUseCase;
import co.com.crediya.usecase.TransactionalSolicitudUseCase;
import lombok.RequiredArgsConstructor;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


@Component
@RequiredArgsConstructor
public class Handler {
    private final TransactionalSolicitudUseCase transactionalSolicitudUseCase;
    private final ObjectMapper objectMapper;
    public Mono<ServerResponse> listenCreateSolicitud(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CreateSolicitudRequest.class)
                .flatMap(request ->
                            Mono.just(objectMapper.map(request, Solicitud.class)))
                .flatMap(this.transactionalSolicitudUseCase::createSolicitudTransactional)
                .flatMap(solicitudCreada ->
                        ServerResponse.created(serverRequest.uri())
                                .bodyValue(objectMapper.map(solicitudCreada, CreateSolicitudResponse.class))
                );
    }
}
