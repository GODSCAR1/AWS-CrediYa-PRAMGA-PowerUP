package co.com.crediya.api;

import co.com.crediya.api.dto.CreateUsuarioDtoRequest;
import co.com.crediya.api.dto.CreateUsuarioDtoResponse;
import co.com.crediya.api.dto.SearchUsuarioResponse;
import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.usuario.TransactionalUsuarioUseCase;
import co.com.crediya.usecase.usuario.UsuarioUseCase;
import co.com.crediya.usecase.usuario.exception.UsuarioNotFoundException;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
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

    private final UsuarioUseCase usuarioUseCase;

    private final ObjectMapper objectMapper;

    public Mono<ServerResponse> listenCreateUsuario(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(CreateUsuarioDtoRequest.class)
                .flatMap(createUsuarioDtoRequest ->
                        Mono.just(objectMapper.map(createUsuarioDtoRequest, Usuario.class)))
                .flatMap(this.transactionalUsuarioUseCase::createUsuarioTransactional)
                .flatMap(usuarioCreado ->
                        Mono.just(objectMapper.map(usuarioCreado, CreateUsuarioDtoResponse.class)))
                .flatMap(createUsuarioDtoResponse ->
                        ServerResponse.created(serverRequest.uri())
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(createUsuarioDtoResponse))
                .onErrorResume(error -> ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("error", error.getMessage())));
    }

    public Mono<ServerResponse> listenSearchUsuario(ServerRequest serverRequest) {
        String email = serverRequest.pathVariable("email");
        return this.usuarioUseCase.searchUsuario(email)
                .flatMap(confirm ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(SearchUsuarioResponse.builder().found(confirm).build()))
                .onErrorResume(UsuarioNotFoundException.class, error -> ServerResponse.status(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("error", error.getMessage())))
                .onErrorResume(UsuarioValidationException.class, error -> ServerResponse.status(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("error", error.getMessage())));
    }

}
