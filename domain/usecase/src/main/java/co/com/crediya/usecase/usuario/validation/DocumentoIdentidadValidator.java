package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.usuario.Validator;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

//TODO: Crear una excepcion personalizada
@RequiredArgsConstructor
public class DocumentoIdentidadValidator implements Validator<Usuario> {
    private final UsuarioRepository usuarioRepository;
    @Override
    public Mono<Void> validate(Usuario usuario) {
        if (usuario.getDocumentoIdentidad() == null || usuario.getDocumentoIdentidad().isBlank()) {
            return Mono.error(new RuntimeException());
        }
        return usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())
                .flatMap(u -> Mono.error(new RuntimeException()));

    }
}
