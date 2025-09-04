package co.com.crediya.api;

import co.com.crediya.api.dto.*;
import co.com.crediya.model.LoginRequest;
import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.login.LoginUseCase;
import co.com.crediya.usecase.usuario.TransactionalUsuarioUseCase;
import co.com.crediya.usecase.usuario.UsuarioUseCase;
import lombok.RequiredArgsConstructor;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;


@Component
@RequiredArgsConstructor
public class Handler {

    private final TransactionalUsuarioUseCase transactionalUsuarioUseCase;

    private final LoginUseCase loginUseCase;

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
                                .bodyValue(createUsuarioDtoResponse));
    }

    public Mono<ServerResponse> listenSearchUsuariosByEmails(ServerRequest serverRequest) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_NDJSON)  // ← NDJSON streaming
                .body(serverRequest.bodyToMono(UsuarioEmailRequest.class)
                                .flatMapMany(req -> usuarioUseCase.getAllUsuariosByEmails(req.getEmails()))
                                .map(usuario -> objectMapper.map(usuario, UsuarioEmailResponse.class)),
                        UsuarioEmailResponse.class);
    }

    public Mono<ServerResponse> listenLogin(ServerRequest serverRequest) {
        return serverRequest.bodyToMono(LoginDtoRequest.class)
                .flatMap(loginDtoRequest ->
                        Mono.just(objectMapper.map(loginDtoRequest, LoginRequest.class)))
                .flatMap(this.loginUseCase::login)
                .flatMap(loginResponse ->
                        ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(objectMapper.map(loginResponse, LoginDtoResponse.class)));
    }

}
