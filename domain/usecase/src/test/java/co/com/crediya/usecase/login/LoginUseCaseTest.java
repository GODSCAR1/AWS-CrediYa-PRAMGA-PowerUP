package co.com.crediya.usecase.login;

import co.com.crediya.model.LoginRequest;
import co.com.crediya.model.Usuario;
import co.com.crediya.model.gateways.TokenGenerator;
import co.com.crediya.model.gateways.PasswordEncoder;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.login.composite.LoginValidationComposite;
import co.com.crediya.usecase.login.exception.LoginValidationException;
import co.com.crediya.usecase.login.validation.ContrasenaLoginValidator;
import co.com.crediya.usecase.login.validation.EmailLoginValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoginUseCaseTest {
    private LoginUseCase loginUseCase;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenGenerator tokenGenerator;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        LoginValidationComposite loginValidationComposite =
                new LoginValidationComposite(new EmailLoginValidator(), new ContrasenaLoginValidator());


        // Crear el UseCase con todas sus dependencias
        loginUseCase = new LoginUseCase(
                usuarioRepository,
                passwordEncoder,
                tokenGenerator,
                loginValidationComposite);

        loginRequest = LoginRequest.builder()
                .email("oscar@gmail.com")
                .contrasena("123456")
                .build();
    }

    @Test
    void mustFailWhenEmailIsNull() {
        loginRequest.setEmail(null);
        StepVerifier.create(loginUseCase.login(loginRequest))
                .expectErrorMatches(ex ->
                        ex instanceof LoginValidationException
                                && "El email es obligatorio".equals(ex.getMessage()))
                .verify();
    }
    @Test
    void mustFailWhenEmailIsBlank() {
        loginRequest.setEmail("");
        StepVerifier.create(loginUseCase.login(loginRequest))
                .expectErrorMatches(ex ->
                        ex instanceof LoginValidationException
                                && "El email es obligatorio".equals(ex.getMessage()))
                .verify();
    }

    @Test
    void mustFailWhenEmailIsInvalid() {
        loginRequest.setEmail("oscar.com");
        StepVerifier.create(loginUseCase.login(loginRequest))
                .expectErrorMatches(ex ->
                        ex instanceof LoginValidationException
                                && "El email es invalido".equals(ex.getMessage()))
                .verify();
    }

    @Test
    void mustFailWhenPasswordIsNull(){
        loginRequest.setContrasena(null);
        StepVerifier.create(loginUseCase.login(loginRequest))
                .expectErrorMatches(ex ->
                        ex instanceof LoginValidationException
                                && "La contrasena es obligatoria".equals(ex.getMessage()))
                .verify();
    }

    @Test
    void mustFailWhenPasswordIsBlank(){
        loginRequest.setContrasena("");
        StepVerifier.create(loginUseCase.login(loginRequest))
                .expectErrorMatches(ex ->
                        ex instanceof LoginValidationException
                                && "La contrasena es obligatoria".equals(ex.getMessage()))
                .verify();
    }

    @Test
    void mustFailWhenEmailIsNotFound(){
        when(this.usuarioRepository.findByEmail(loginRequest.getEmail())).thenReturn(Mono.empty());
        StepVerifier.create(loginUseCase.login(loginRequest))
                .expectErrorMatches(ex ->
                        ex instanceof LoginValidationException
                                && "Credenciales incorrectas".equals(ex.getMessage()))
                .verify();
    }

    @Test
    void mustFailWhenPasswodDoesNotMatch(){
        when(this.usuarioRepository.findByEmail(loginRequest.getEmail())).thenReturn(Mono.just(Usuario.builder().build()));
        when(this.passwordEncoder.matches(any(String.class),any(String.class))).thenReturn(false);
        StepVerifier.create(loginUseCase.login(loginRequest))
                .expectErrorMatches(ex ->
                        ex instanceof LoginValidationException
                                && "Credenciales incorrectas".equals(ex.getMessage()))
                .verify();
    }
    @Test
    void mustGenerateTokenWhenLoginIsSuccess(){
        when(this.usuarioRepository.findByEmail(loginRequest.getEmail())).thenReturn(Mono.just(Usuario.builder().contrasena("hashed").build()));
        when(this.passwordEncoder.matches(loginRequest.getContrasena(),"hashed")).thenReturn(true);
        when(this.tokenGenerator.generateToken(any(Usuario.class))).thenReturn(Mono.just("token"));
        StepVerifier.create(loginUseCase.login(loginRequest))
                .assertNext( u ->
                        assertEquals("token",u.getToken()))
                .verifyComplete();
    }
}
