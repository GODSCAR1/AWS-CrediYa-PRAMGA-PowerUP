package co.com.crediya.model.estados.gateways;

import co.com.crediya.model.estados.Estado;
import reactor.core.publisher.Mono;

public interface EstadoRepository {

    Mono<Estado> findByNombre(String nombre);
}
