package co.com.crediya.r2dbc;


import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.r2dbc.entity.SolicitudEntity;
import co.com.crediya.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
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
    public Mono<Solicitud> update(Solicitud solicitud) {
        SolicitudEntity entity = toData(solicitud);
        entity.markNotNew();
        return repository.save(entity).map(this::toEntity);
    }

    @Override
    public Mono<Solicitud> save(Solicitud solicitud) {
        return super.save(solicitud);
    }


    @Override
    public Flux<Solicitud> findByEmailAndEstadoNombre(String email, String estadoNombre) {
        return repository.findByEmailAndEstadoNombre(email, estadoNombre)
                .map(entity -> mapper.map(entity, Solicitud.class));
    }


}
