package co.com.crediya.model.gateways;

import co.com.crediya.model.Usuario;
import reactor.core.publisher.Mono;
// Cambiar el nombre que no esté ligado a una tecnologia.
public interface TokenGenerator {
    Mono<String> generateToken(Usuario usuario);
}
