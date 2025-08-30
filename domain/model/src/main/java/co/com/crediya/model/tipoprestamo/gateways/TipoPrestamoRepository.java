package co.com.crediya.model.tipoprestamo.gateways;

import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import reactor.core.publisher.Mono;

public interface TipoPrestamoRepository {
    Mono<TipoPrestamo> findByNombre(String nombre);
}
