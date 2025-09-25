package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.model.gateways.UsuarioRepository;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import co.com.crediya.usecase.usuario.message.ValidationMessage;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DocumentoIdentidadValidator implements Validator<Usuario> {
    private final UsuarioRepository usuarioRepository;
    @Override
    public Mono<Void> validate(Usuario usuario) {
        if (usuario.getDocumentoIdentidad() == null || usuario.getDocumentoIdentidad().isBlank()) {
            return Mono.error(new UsuarioValidationException(ValidationMessage.DOCUMENTO_IDENTIDAD_OBLIGATORIO.getMensaje()));
        }
        return usuarioRepository.findByDocumentoIdentidad(usuario.getDocumentoIdentidad())
                .flatMap(u -> Mono.error(new UsuarioValidationException(ValidationMessage.DOCUMENTO_IDENTIDAD_EXISTENTE.getMensaje())));

    }
}
