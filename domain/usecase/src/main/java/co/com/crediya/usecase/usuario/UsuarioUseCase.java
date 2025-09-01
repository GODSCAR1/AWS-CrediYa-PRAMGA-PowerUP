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
import reactor.core.publisher.Mono;
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
                        this.rolRepository.findByNombre("Solicitante")
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

    public Mono<Usuario> updateUsuarioToCliente(String email){
        Mono<Usuario> usuarioMono = this.usuarioRepository.findByEmail(email)
                .switchIfEmpty(Mono.defer(() -> {
                    log.severe("Usuario no encontrado");
                    return Mono.error(new UsuarioNotFoundException("Usuario no encontrado"));
                }));
        Mono<Rol> rolMono = this.rolRepository.findByNombre("CLIENTE")
                .switchIfEmpty(Mono.defer(() -> {
                    log.severe("Rol no encontrado");
                    return Mono.error(new RolValidationException("Rol no encontrado"));
                }));
        return Mono.zip(usuarioMono, rolMono)
                .flatMap(tuple -> {
                    Usuario usuario = tuple.getT1();
                    Rol rol = tuple.getT2();
                    usuario.setIdRol(rol.getId());
                    return this.usuarioRepository.save(usuario);
                })
                .doOnSuccess(
                        usuarioActualizado -> log.info(String.format("Usuario %s actualizado a Cliente exitosamente", usuarioActualizado.getEmail()))
                );
    }

}
