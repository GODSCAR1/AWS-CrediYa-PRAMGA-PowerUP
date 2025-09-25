package co.com.crediya.usecase.usuario;

import co.com.crediya.model.Rol;
import co.com.crediya.model.Usuario;
import co.com.crediya.model.gateways.PasswordEncoder;
import co.com.crediya.model.gateways.RolRepository;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.usuario.composite.UsuarioValidationComposite;
import co.com.crediya.usecase.usuario.exception.RolValidationException;
import co.com.crediya.usecase.usuario.exception.UsuarioNotFoundException;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import co.com.crediya.usecase.usuario.message.RolMessage;
import co.com.crediya.usecase.usuario.message.UsuarioMessage;
import co.com.crediya.usecase.usuario.message.ValidationMessage;
import co.com.crediya.usecase.usuario.validation.EmailValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@Log
public class UsuarioUseCase {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioValidationComposite usuarioValidationComposite;
    private final PasswordEncoder passwordEncoder;

    public Mono<Usuario> createUsuario(Usuario usuario){
        return this.usuarioValidationComposite.validate(usuario)
                .doOnError(
                        error -> log.severe(String.format(ValidationMessage.ERROR_GENERICO.getMensaje(),  error.getMessage()))
                )
                .then(Mono.defer(() ->
                        this.rolRepository.findByNombre("CLIENTE")
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.severe(RolMessage.ROL_NO_ENCONTRADO.getMensaje());
                                    return Mono.error(new RolValidationException(RolMessage.ROL_NO_ENCONTRADO.getMensaje()));
                                }))
                                .flatMap(rol -> {
                                    usuario.setIdRol(rol.getId());
                                    usuario.setContrasena(this.passwordEncoder.encode(usuario.getContrasena()));
                                    return this.usuarioRepository.save(usuario);
                                })
                ))
                .doOnSuccess(
                        usuarioGuardado -> log.info(String.format(UsuarioMessage.USUARIO_CREADO.getMensaje(), usuarioGuardado.getEmail()))
                );
    }

    public Flux<Usuario> getAllUsuariosByEmails(List<String> emails){
        return this.usuarioRepository.findAllByEmail(emails)
                .switchIfEmpty(Mono.defer(() -> {
                    log.severe(UsuarioMessage.USUARIOS_NO_ENCONTRADOS.getMensaje());
                    return Mono.error(new UsuarioNotFoundException(UsuarioMessage.USUARIOS_NO_ENCONTRADOS.getMensaje()));
                }))
                .doOnComplete(() -> log.info(UsuarioMessage.USUARIOS_OBTENIDOS.getMensaje()));
    }

}
