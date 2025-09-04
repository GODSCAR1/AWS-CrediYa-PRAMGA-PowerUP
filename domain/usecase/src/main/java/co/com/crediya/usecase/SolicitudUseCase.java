package co.com.crediya.usecase;

import co.com.crediya.model.estados.Estado;
import co.com.crediya.model.estados.gateways.EstadoRepository;
import co.com.crediya.model.solicitud.PagedSolicitud;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.SolicitudInfo;
import co.com.crediya.model.solicitud.gateways.CustomSolicitudRepository;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.crediya.model.usuario.Usuario;
import co.com.crediya.model.usuario.gateways.UsuarioConsumer;
import co.com.crediya.usecase.composite.SolicitudValidationComposite;
import co.com.crediya.usecase.exception.EstadoValidationException;
import co.com.crediya.usecase.exception.SecurityValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Log
public class SolicitudUseCase {
    private final SolicitudValidationComposite solicitudValidationComposite;
    private final SolicitudRepository solicitudRepository;
    private final EstadoRepository estadoRepository;
    private final TipoPrestamoRepository tipoPrestamoRepository;
    private final CustomSolicitudRepository customSolicitudRepository;
    private final UsuarioConsumer usuarioConsumer;

    public Mono<Solicitud> createSolicitud(Solicitud solicitud, String emailHeader) {
        return this.solicitudValidationComposite.validate(solicitud)
                .doOnError(
                        error -> log.warning("Error en la validacion de la solicitud: " + error.getMessage())
                )
                .then(Mono.defer(() -> {
                    Mono<String> estadoMono = this.estadoRepository.findByNombre("Pendiente de revision")
                            .switchIfEmpty(Mono.defer(() -> {
                                log.warning("No se encontró el estado 'Pendiente de revision'");
                                return Mono.error(new EstadoValidationException("El estado 'Pendiente de revision' no existe"));
                            }))
                            .map(Estado::getId);

                    Mono<String> tipoPrestamoMono = this.tipoPrestamoRepository.findByNombre(solicitud.getNombreTipoPrestamo())
                            .map(TipoPrestamo::getId);
                    
                    return Mono.zip(estadoMono, tipoPrestamoMono)
                            .flatMap(tuple -> {
                                String estadoId = tuple.getT1();
                                String tipoPrestamoId = tuple.getT2();
                                solicitud.setEmail(emailHeader);
                                solicitud.setIdEstado(estadoId);
                                solicitud.setIdTipoPrestamo(tipoPrestamoId);
                                return this.solicitudRepository.save(solicitud)
                                        .map(solicitudGuardada -> {
                                            solicitudGuardada.setNombreTipoPrestamo(solicitud.getNombreTipoPrestamo());
                                            return solicitudGuardada;
                                        });
                            })
                            .doOnSuccess(solicitudGuardada ->
                                    log.info(String.format("Solicitud %s creada exitosamente", solicitudGuardada.getId())));

                }));
    }

    public Mono<PagedSolicitud> getSolicitudPaged(String nombreEstado, int page, int size, String sortBy, String sortDirection) {

        Mono<Long> totalCountMono = this.customSolicitudRepository.countByNombreEstado(nombreEstado);

        Flux<SolicitudInfo> solicitudFlux = this.customSolicitudRepository.findByAllByEstado(
                nombreEstado, page, size, sortBy, sortDirection);

        Mono<List<SolicitudInfo>> solicitudesListMono = solicitudFlux.collectList();

        return Mono.zip(totalCountMono, solicitudesListMono)
                .flatMap(tuple -> {
                    Long totalCount = tuple.getT1();
                    List<SolicitudInfo> solicitudesList = tuple.getT2();

                    if (solicitudesList.isEmpty()) {
                        return Mono.just(buildEmptyPagedSolicitud(page, size));
                    }

                    List<String> uniqueEmails = solicitudesList.stream()
                            .map(SolicitudInfo::getEmail)
                            .distinct()
                            .toList();

                    Flux<Usuario> usuarioFlux = this.usuarioConsumer.getUsuariosByEmails(uniqueEmails);

                    return usuarioFlux.collectList()
                            .map(usuariosList -> {
                                        Map<String, Usuario> usuarioMap = usuariosList.stream()
                                                .collect(Collectors.toMap(Usuario::getEmail, Function.identity()));

                                List<SolicitudInfo> solicitudesEnriquecidas = solicitudesList.stream()
                                        .map(solicitud -> {
                                            Usuario usuario = usuarioMap.get(solicitud.getEmail());
                                            return enrichSolicitudWithUsuario(solicitud, usuario);
                                        })
                                        .toList();

                                return buildPagedSolicitud(solicitudesEnriquecidas, page, size, totalCount);
                            });
                });


    }

    private SolicitudInfo enrichSolicitudWithUsuario(SolicitudInfo solicitud, Usuario usuario) {

        solicitud.setNombre(usuario.getNombre());
        solicitud.setSalarioBase(usuario.getSalarioBase());
        return solicitud;

    }

    private PagedSolicitud buildPagedSolicitud(List<SolicitudInfo> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PagedSolicitud.builder()
                .content(content)
                .pageNumber(page)
                .pageSize(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }

    private PagedSolicitud buildEmptyPagedSolicitud(int page, int size) {
        return PagedSolicitud.builder()
                .content(Collections.emptyList())
                .pageNumber(page)
                .pageSize(size)
                .totalElements(0L)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();
    }


}
