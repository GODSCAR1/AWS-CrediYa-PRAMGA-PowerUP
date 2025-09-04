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
                        error -> log.severe(String.format("Error de validacion: %s",  error.getMessage()))
                )
                .then(Mono.defer(() ->
                        this.rolRepository.findByNombre("CLIENTE")
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.severe("Rol no encontrado");
                                    return Mono.error(new RolValidationException("Rol no encontrado"));
                                }))
                                .flatMap(rol -> {
                                    usuario.setIdRol(rol.getId());
                                    usuario.setContrasena(this.passwordEncoder.encode(usuario.getContrasena()));
                                    return this.usuarioRepository.save(usuario);
                                })
                ))
                .doOnSuccess(
                        usuarioGuardado -> log.info(String.format("Usuario %s creado exitosamente", usuarioGuardado.getEmail()))
                );
    }

    public Flux<Usuario> getAllUsuariosByEmails(List<String> emails){
        return this.usuarioRepository.findAllByEmail(emails)
                .switchIfEmpty(Mono.defer(() -> {
                    log.severe("No se encontraron usuarios");
                    return Mono.error(new UsuarioNotFoundException("No se encontraron usuarios"));
                }))
                .doOnComplete(() -> log.info("Usuarios obtenidos exitosamente"));
    }

}
