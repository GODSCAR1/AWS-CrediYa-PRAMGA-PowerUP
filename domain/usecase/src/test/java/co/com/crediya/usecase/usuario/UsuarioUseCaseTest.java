package co.com.crediya.usecase.usuario;

import co.com.crediya.model.Rol;
import co.com.crediya.model.Usuario;
import co.com.crediya.model.gateways.RolRepository;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.usuario.composite.UsuarioValidationComposite;
import co.com.crediya.usecase.usuario.exception.RolValidationException;
import co.com.crediya.usecase.usuario.exception.UsuarioNotFoundException;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import co.com.crediya.usecase.usuario.validation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioUseCaseTest {

    private UsuarioUseCase usuarioUseCase;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        UsuarioValidationComposite usuarioValidationComposite =
                this.getUsuarioValidationComposite();

        // Crear el UseCase con todas sus dependencias
        usuarioUseCase = new UsuarioUseCase(usuarioRepository, rolRepository, usuarioValidationComposite);

        usuario = Usuario.builder()
                .nombre("Oscar")
                .apellido("Godscar")
                .email("oscar@gmail.com")
                .documentoIdentidad("12345678")
                .contrasena("123456")
                .salarioBase(new BigDecimal(1000000))
                .build();
    }

    private UsuarioValidationComposite getUsuarioValidationComposite() {
        NombreValidator nombreValidator = new NombreValidator();
        ApellidoValidator apellidoValidator = new ApellidoValidator();
        EmailValidator emailValidator = new EmailValidator(usuarioRepository);
        DocumentoIdentidadValidator documentoIdentidadValidator = new DocumentoIdentidadValidator(usuarioRepository);
        ContrasenaValidator contrasenaValidator = new ContrasenaValidator();
        SalarioBaseValidator salarioBaseValidator = new SalarioBaseValidator();

        return new UsuarioValidationComposite(
                nombreValidator,
                apellidoValidator,
                emailValidator,
                documentoIdentidadValidator,
                contrasenaValidator,
                salarioBaseValidator
        );
    }

    @Test
    void mustFailWhenNameIsNull() {
        usuario.setNombre(null);
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El nombre es obligatorio".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
    @Test
    void mustFailWhenNameIsBlank() {
        usuario.setNombre("");
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El nombre es obligatorio".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
    @Test
    void mustFailWhenApellidoIsNull() {
        usuario.setApellido(null);
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El apellido es obligatorio".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void mustFailWhenApellidoIsBlank() {
        usuario.setApellido("");
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El apellido es obligatorio".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void mustFailWhenEmailIsNull() {
        usuario.setEmail(null);
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El email es obligatorio".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
    @Test
    void mustFailWhenEmailIsBlank() {
        usuario.setEmail("");
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El email es obligatorio".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
    @Test
    void mustFailWhenEmailIsInvalid() {
        usuario.setEmail("random");
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El email es invalido".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void mustFailWhenEmailAlreadyExists() {
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.just(usuario));
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El email ya existe".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void mustFailWhenDocumentoIdentidadIsNull() {
        usuario.setDocumentoIdentidad(null);
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El documento de identidad es obligatorio".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
    @Test
    void mustFailWhenDocumentoIdentidadIsBlank() {
        usuario.setDocumentoIdentidad("");
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El documento de identidad es obligatorio".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
    @Test
    void mustFailWhenDocumentoIdentidadAlreadyExists() {
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.just(usuario));
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El documento de identidad ya existe".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
    @Test
    void mustFailWhenContrasenaIsNull() {
        usuario.setContrasena(null);
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "La contrasena es obligatoria".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void mustFailWhenContrasenaIsBlank() {
        usuario.setContrasena("");
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "La contrasena es obligatoria".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void mustFailWhenSalarioBaseIsLessIsNull() {
        usuario.setSalarioBase(null);
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El salario base es obligatorio".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }


    @Test
    void mustFailWhenSalarioBaseIsLessThanMinimum() {
        usuario.setSalarioBase(BigDecimal.valueOf(-1));
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El salario base debe ser mayor o igual a 0".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }
    @Test
    void mustFailWhenSalarioBaseIsMoreThanMaximum() {
        BigDecimal maxSalary = BigDecimal.valueOf(15000000);
        usuario.setSalarioBase(maxSalary.add(BigDecimal.ONE));
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El salario base debe ser menor o igual a 15000000".equals(ex.getMessage()))
                .verify();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void passAllValidationAndFailsWhenNoRoleIsFound(){
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        when(this.rolRepository.findByNombre("Solicitante")).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .expectErrorMatches(ex ->
                        ex instanceof RolValidationException
                                && "Rol no encontrado".equals(ex.getMessage()))
                .verify();
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void passAllValidationAndRoleIsFound(){
        Rol rol = Rol.builder()
                .id("1")
                .nombre("Solicitante")
                .descripcion("Rol de usuario")
                .build();
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        when(this.usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())).thenReturn(Mono.empty());
        when(this.rolRepository.findByNombre("Solicitante")).thenReturn(Mono.just(rol));
        when(this.usuarioRepository.save(usuario)).thenReturn(Mono.just(usuario));
        StepVerifier.create(usuarioUseCase.createUsuario(usuario))
                .assertNext( u -> assertEquals("1",u.getIdRol()))
                .verifyComplete();
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void searchUsuarioMustFailWhenEmailIsNull(){
        String email = null;
        StepVerifier.create(usuarioUseCase.searchUsuario(email))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El email es obligatorio".equals(ex.getMessage()))
                .verify();
        verify(usuarioRepository, never()).findByEmail(any(String.class));
    }

    @Test
    void searchUsuarioMustFailWhenEmailIsBlank(){
        String email = "";
        StepVerifier.create(usuarioUseCase.searchUsuario(email))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioValidationException
                                && "El email es obligatorio".equals(ex.getMessage()))
                .verify();
        verify(usuarioRepository, never()).findByEmail(any(String.class));
    }

    @Test
    void searchUsuarioMustFailWhenUserIsNotFound(){
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.empty());
        StepVerifier.create(usuarioUseCase.searchUsuario(usuario.getEmail()))
                .expectErrorMatches(ex ->
                        ex instanceof UsuarioNotFoundException
                                && "Usuario no encontrado".equals(ex.getMessage()))
                .verify();
    }

    @Test
    void searchUsuarioMustSuccess(){
        when(this.usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Mono.just(usuario));
        StepVerifier.create(usuarioUseCase.searchUsuario(usuario.getEmail()))
                .assertNext(u -> assertEquals(Boolean.TRUE,u))
                .verifyComplete();
    }


}
