package co.com.crediya.api;

import co.com.crediya.api.dto.CreateUsuarioDtoRequest;
import co.com.crediya.api.dto.CreateUsuarioDtoResponse;
import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.usuario.TransactionalUsuarioUseCase;
import co.com.crediya.usecase.usuario.UsuarioUseCase;
import lombok.RequiredArgsConstructor;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class Handler {

    private final TransactionalUsuarioUseCase transactionalUsuarioUseCase;

    private final ObjectMapper objectMapper;

    public Mono<ServerResponse> listenCreateUsuario(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CreateUsuarioDtoRequest.class)
                .flatMap(createUsuarioDtoRequest ->
                        Mono.just(objectMapper.map(createUsuarioDtoRequest, Usuario.class)))
                .flatMap(this.transactionalUsuarioUseCase::createUsuarioTransactional)
                .flatMap(usuarioCreado ->
                        Mono.just(objectMapper.map(usuarioCreado, CreateUsuarioDtoResponse.class)))
                .flatMap(createUsuarioDtoResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(createUsuarioDtoResponse))
                .onErrorResume(error -> ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("error", error.getMessage())));
    }

}
