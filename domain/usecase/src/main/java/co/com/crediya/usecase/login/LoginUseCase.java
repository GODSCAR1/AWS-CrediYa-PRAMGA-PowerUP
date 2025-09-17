package co.com.crediya.usecase.login;

import co.com.crediya.model.LoginRequest;
import co.com.crediya.model.LoginResponse;
import co.com.crediya.model.gateways.TokenGenerator;
import co.com.crediya.model.gateways.PasswordEncoder;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.login.composite.LoginValidationComposite;
import co.com.crediya.usecase.login.exception.LoginValidationException;

import co.com.crediya.usecase.login.message.LoginMessage;
import co.com.crediya.usecase.login.message.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Log
public class LoginUseCase {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final LoginValidationComposite loginValidationComposite;

    public Mono<LoginResponse> login(LoginRequest loginRequest) {
        return this.loginValidationComposite.validate(loginRequest)
                .doOnError(
                        error -> log.severe(String.format(ValidationMessage.ERROR_GENERICO.getMensaje(),  error.getMessage()))
                )
                .then(Mono.defer(() ->
                    this.usuarioRepository.findByEmail(loginRequest.getEmail())
                            .switchIfEmpty(Mono.defer(() -> {
                                log.severe(LoginMessage.USUARIO_NO_ENCONTRADO.getMensaje());
                                return Mono.error(new LoginValidationException(LoginMessage.CREDENCIALES_INVALIDAS.getMensaje()));
                            }))
                            .flatMap(usuario -> {
                                if(!passwordEncoder.matches(loginRequest.getContrasena(), usuario.getContrasena())) {
                                    return Mono.error(new LoginValidationException(LoginMessage.CREDENCIALES_INVALIDAS.getMensaje()));
                                }
                                return this.tokenGenerator.generateToken(usuario)
                                        .map(token -> LoginResponse.builder().token(token).build());
                            })
                ))
                .doOnSuccess(
                        loginResponse -> log.info(String.format(LoginMessage.USUARIO_LOGEADO.getMensaje(), loginRequest.getEmail()))
                );
    }
}
