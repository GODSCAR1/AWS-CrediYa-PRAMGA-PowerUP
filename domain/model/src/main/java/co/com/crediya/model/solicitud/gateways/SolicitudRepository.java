package co.com.crediya.model.solicitud.gateways;

import co.com.crediya.model.solicitud.Solicitud;
import reactor.core.publisher.Mono;

public interface SolicitudRepository {
    Mono<Solicitud> save(Solicitud solicitud);
}
