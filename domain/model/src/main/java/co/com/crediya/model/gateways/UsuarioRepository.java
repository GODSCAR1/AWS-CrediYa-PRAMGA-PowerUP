package co.com.crediya.model.gateways;

import co.com.crediya.model.Usuario;
import reactor.core.publisher.Mono;

public interface UsuarioRepository {
    Mono<Usuario> save(Usuario usuario);

    Mono<Usuario> findByEmail(String email);
}
