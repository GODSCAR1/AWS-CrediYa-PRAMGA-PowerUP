package co.com.crediya.model.gateways;

import co.com.crediya.model.Usuario;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface UsuarioRepository {
    Mono<Usuario> save(Usuario usuario);

    Mono<Usuario> findByEmail(String email);

    Mono<Usuario> findByDocumentoIdentidad(String documentoIdentidad);

    Flux<Usuario> findAllByEmail(List<String> emails);
}
