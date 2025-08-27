package co.com.crediya.api;

import co.com.crediya.api.dto.CreateUserDtoRequest;
import co.com.crediya.api.dto.CreateUserDtoResponse;
import co.com.crediya.model.Usuario;
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

    private final UsuarioUseCase usuarioUseCase;

    private final ObjectMapper objectMapper;

    public Mono<ServerResponse> listenCreateUsuario(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CreateUserDtoRequest.class)
                .flatMap(createUserDtoRequest ->
                        Mono.just(objectMapper.map(createUserDtoRequest, Usuario.class)))
                .flatMap(this.usuarioUseCase::createUsuario)
                .flatMap(usuarioCreado ->
                        Mono.just(objectMapper.map(usuarioCreado, CreateUserDtoResponse.class)))
                .flatMap(createUserDtoResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(createUserDtoResponse))
                .onErrorResume(error -> ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("error", error.getMessage())));
    }

}
