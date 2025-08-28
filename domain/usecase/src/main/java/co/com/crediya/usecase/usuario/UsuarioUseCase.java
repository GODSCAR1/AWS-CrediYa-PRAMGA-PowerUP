package co.com.crediya.usecase.usuario;

import co.com.crediya.model.Usuario;
import co.com.crediya.model.gateways.RolRepository;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.usuario.composite.UsuarioValidationComposite;
import co.com.crediya.usecase.usuario.exception.RolValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Mono;
@RequiredArgsConstructor
@Log
public class UsuarioUseCase {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioValidationComposite usuarioValidationComposite;
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
                                    return this.usuarioRepository.save(usuario);
                                })
                ))
                .doOnSuccess(
                        usuarioGuardado -> log.info(String.format("Usuario %s creado exitosamente", usuarioGuardado.getEmail()))
                );
    }
}
