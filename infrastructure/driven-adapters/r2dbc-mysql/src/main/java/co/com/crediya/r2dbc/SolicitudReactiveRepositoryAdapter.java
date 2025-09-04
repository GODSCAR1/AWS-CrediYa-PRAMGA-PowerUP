package co.com.crediya.r2dbc;

import co.com.crediya.model.solicitud.PagedSolicitud;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.r2dbc.entity.SolicitudEntity;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class SolicitudReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Solicitud/* change for domain model */,
        SolicitudEntity/* change for adapter model */,
        String,
        SolicitudReactiveRepository
> implements SolicitudRepository {
    public SolicitudReactiveRepositoryAdapter(SolicitudReactiveRepository repository, ObjectMapper mapper) {
        /**
         *  Could be use mapper.mapBuilder if your domain model implement builder pattern
         *  super(repository, mapper, d -> mapper.mapBuilder(d,ObjectModel.ObjectModelBuilder.class).build());
         *  Or using mapper.map with the class of the object model
         */
        super(repository, mapper, d -> mapper.map(d, Solicitud.class/* change for domain model */));
    }

    @Override
    public Mono<Solicitud> save(Solicitud solicitud) {
        return super.save(solicitud);
    }




}
