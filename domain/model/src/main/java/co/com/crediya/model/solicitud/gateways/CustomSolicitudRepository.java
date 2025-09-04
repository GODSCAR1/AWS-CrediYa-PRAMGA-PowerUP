package co.com.crediya.model.solicitud.gateways;

import co.com.crediya.model.solicitud.SolicitudInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomSolicitudRepository {
    Flux<SolicitudInfo> findByAllByEstado(String nombreEstado, int page, int size, String sortBy, String sortDirection);

    Mono<Long> countByNombreEstado(String nombreEstado);
}
