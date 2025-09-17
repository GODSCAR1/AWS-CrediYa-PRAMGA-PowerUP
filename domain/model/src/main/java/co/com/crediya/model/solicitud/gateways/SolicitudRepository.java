package co.com.crediya.model.solicitud.gateways;

import co.com.crediya.model.solicitud.PagedSolicitud;
import co.com.crediya.model.solicitud.Solicitud;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SolicitudRepository {
    Mono<Solicitud> save(Solicitud solicitud);
    Mono<Solicitud> findById(String id);

    Flux<Solicitud> findByEmailAndEstadoNombre(String email, String estadoNombre);

    Mono<Solicitud> update(Solicitud solicitud);

}
