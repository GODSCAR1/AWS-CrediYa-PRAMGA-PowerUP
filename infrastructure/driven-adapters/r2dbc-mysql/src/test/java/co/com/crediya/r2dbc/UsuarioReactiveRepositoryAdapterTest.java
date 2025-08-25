package co.com.crediya.r2dbc;

import co.com.crediya.model.Usuario;
import co.com.crediya.r2dbc.entity.UsuarioEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioReactiveRepositoryAdapterTest {

    @InjectMocks
    UsuarioReactiveRepositoryAdapter repositoryAdapter;

    @Mock
    UsuarioReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    private final Usuario usuario = Usuario.builder()
            .id("1")
            .nombre("Nombre1")
            .apellido("Apellido1")
            .build();

    private final UsuarioEntity usuarioEntity = UsuarioEntity.builder()
            .id("1")
            .nombre("Nombre1")
            .apellido("Apellido1")
            .build();;

    @Test
    void mustFindValueById() {
        when(mapper.map(usuarioEntity, Usuario.class)).thenReturn(usuario);
        when(repository.findById("1")).thenReturn(Mono.just(usuarioEntity));

        Mono<Usuario> result = repositoryAdapter.findById("1");

        StepVerifier.create(result)
                .expectNextMatches(u -> u.equals(usuario))
                .verifyComplete();
    }

    @Test
    void mustFindAllValues() {
        when(mapper.map(usuarioEntity, Usuario.class)).thenReturn(usuario);
        when(repository.findAll()).thenReturn(Flux.just(usuarioEntity));


        Flux<Usuario> result = repositoryAdapter.findAll();

        StepVerifier.create(result)
                .expectNext(usuario)
                .verifyComplete();
    }

    @Test
    void mustSaveValue() {
        when(mapper.map(usuarioEntity, Usuario.class)).thenReturn(usuario);
        when(mapper.map(usuario, UsuarioEntity.class)).thenReturn(usuarioEntity);
        when(repository.save(usuarioEntity)).thenReturn(Mono.just(usuarioEntity));

        Mono<Usuario> result = repositoryAdapter.save(usuario);

        StepVerifier.create(result)
                .expectNext(usuario)
                .verifyComplete();
    }
}
