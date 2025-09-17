package co.com.crediya.usecase;

import co.com.crediya.model.dto.SQSDTO;
import co.com.crediya.model.estados.Estado;
import co.com.crediya.model.estados.gateways.EstadoRepository;
import co.com.crediya.model.events.SolicitudEventCapacidadEndeudamiento;
import co.com.crediya.model.events.gateways.EventPublisherCapacidadEndeudamiento;
import co.com.crediya.model.events.gateways.EventPublisherNotificaciones;
import co.com.crediya.model.events.SolicitudEventNotificaciones;
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
import co.com.crediya.usecase.exception.SolicitudNotFoundException;
import co.com.crediya.usecase.message.SolicitudMessage;
import co.com.crediya.usecase.message.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
    private final EventPublisherNotificaciones eventPublisherNotificaciones;
    private final EventPublisherCapacidadEndeudamiento eventPublisherCapacidadEndeudamiento;
// Crear Solicitudes Nuevas
    public Mono<Solicitud> createSolicitud(Solicitud solicitud, String emailHeader) {
        return this.solicitudValidationComposite.validate(solicitud)
                .doOnError(
                        error -> log.warning(String.format(ValidationMessage.ERROR_GENERICO.getMensaje(), error.getMessage()))
                )
                .then(Mono.defer(() -> {
                    Mono<String> estadoMono = this.estadoRepository.findByNombre("Pendiente de revision")
                            .switchIfEmpty(Mono.defer(() -> {
                                log.warning(SolicitudMessage.ESTADO_PENDIENTE_REVISION_NO_ENCONTRADO.getMensaje());
                                return Mono.error(new EstadoValidationException(SolicitudMessage.ESTADO_PENDIENTE_REVISION_NO_ENCONTRADO.getMensaje()));
                            }))
                            .map(Estado::getId);

                    Mono<TipoPrestamo> tipoPrestamoMono = this.tipoPrestamoRepository.findByNombre(solicitud.getNombreTipoPrestamo());
                    
                    return Mono.zip(estadoMono, tipoPrestamoMono)
                            .flatMap(tuple -> {
                                String estadoId = tuple.getT1();
                                String tipoPrestamoId = tuple.getT2().getId();
                                solicitud.setId(UUID.randomUUID().toString());
                                solicitud.setEmail(emailHeader);
                                solicitud.setIdEstado(estadoId);
                                solicitud.setIdTipoPrestamo(tipoPrestamoId);
                                return this.solicitudRepository.save(solicitud)
                                        .map(solicitudGuardada -> {
                                            solicitudGuardada.setNombreTipoPrestamo(solicitud.getNombreTipoPrestamo());
                                            return solicitudGuardada;
                                        })
                                        .delayUntil(solicitudGuardada ->{
                                            log.info(solicitudGuardada.toString());
                                            if (Boolean.TRUE.equals(tuple.getT2().getValidacionAutomatica())){
                                                return this.usuarioConsumer.getUsuariosByEmails(List.of(emailHeader))
                                                        .take(1)
                                                        .flatMap(usuario -> sendSQSEventCapacidadEndeudamiento(solicitudGuardada, tuple.getT2().getTasaInteres(), usuario.getSalarioBase()));
                                            }
                                            return Mono.empty();
                                        });


                            })
                            .doOnSuccess(solicitudGuardada ->
                                    log.info(String.format(SolicitudMessage.SOLICITUD_CREADA.getMensaje(), solicitudGuardada.getId())));

                }));
    }

    private Mono<BigDecimal> calculateDeudaMensualActual(String email){
        return this.solicitudRepository.findByEmailAndEstadoNombre(email, "Aprobado")
                .flatMap(this::calculateCuotaMensual)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Mono<BigDecimal> calculateCuotaMensual(Solicitud solicitud){
        return this.tipoPrestamoRepository.findById(solicitud.getIdTipoPrestamo())
                .map(tipoPrestamo -> cuotaMensual(solicitud.getMonto(), tipoPrestamo.getTasaInteres(), solicitud.getPlazo()))
                .doOnSuccess(cuota -> log.info("Cuota Mensual: " + cuota.toString()));
    }
    private BigDecimal cuotaMensual(BigDecimal monto, BigDecimal tasaInteres, int plazo) {
        // Convertir la tasa de interés a double para los cálculos de potencia
        tasaInteres = tasaInteres.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        // Calcular (1 + tasa)^plazo
        BigDecimal factorInteres = tasaInteres.add(BigDecimal.ONE).pow(plazo);

        // Calcular el numerador: tasa * (1 + tasa)^plazo
        BigDecimal numerador = tasaInteres.multiply(factorInteres);

        // Calcular el denominador: (1 + tasa)^plazo - 1
        BigDecimal denominador = factorInteres.subtract(BigDecimal.ONE);

        // Calcular la cuota: monto * (numerador / denominador)
        BigDecimal factorCuota = numerador.divide(denominador, 10, RoundingMode.HALF_UP);

        return monto.multiply(factorCuota).setScale(2, RoundingMode.HALF_UP);
    }
// Solicitudes Paginadas
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
                            .flatMap(usuariosList -> {
                                // Crear mapa de usuarios por email
                                Map<String, Usuario> usuarioMap = usuariosList.stream()
                                        .collect(Collectors.toMap(Usuario::getEmail, Function.identity()));

                                // Calcular deudas para cada usuario obtenido usando flatMap
                                return Flux.fromIterable(usuariosList)
                                        .flatMap(usuario ->
                                                this.calculateDeudaMensualActual(usuario.getEmail())
                                                        .map(deuda -> Map.entry(usuario.getEmail(), deuda))
                                                        .onErrorReturn(Map.entry(usuario.getEmail(), BigDecimal.ZERO))
                                        )
                                        .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                                        .map(deudasMap -> {
                                            List<SolicitudInfo> solicitudesEnriquecidas = solicitudesList.stream()
                                                    .map(solicitud -> {
                                                        Usuario usuario = usuarioMap.get(solicitud.getEmail());
                                                        BigDecimal deudaMensual = deudasMap.getOrDefault(solicitud.getEmail(), BigDecimal.ZERO);
                                                        return enrichSolicitud(solicitud, usuario, deudaMensual);
                                                    })
                                                    .toList();

                                            return buildPagedSolicitud(solicitudesEnriquecidas, page, size, totalCount);
                                        });
                            });
                });



    }

    private SolicitudInfo enrichSolicitud(SolicitudInfo solicitud, Usuario usuario, BigDecimal deudaMensual) {
        solicitud.setNombre(usuario.getNombre());
        solicitud.setSalarioBase(usuario.getSalarioBase());
        solicitud.setDeudaTotalMensual(deudaMensual);
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

// Manejo de solicitud Manual.
    public Mono<Solicitud> handleSolicitudManual(String id, Boolean aprobado) {
        return this.solicitudRepository.findById(id)
                .switchIfEmpty(Mono.error(new SolicitudNotFoundException(String.format(SolicitudMessage.SOLICITUD_NO_ENCONTRADA.getMensaje(), id))))
                .flatMap(solicitud ->
                        this.estadoRepository.findById(solicitud.getIdEstado())
                                .flatMap(estado -> {
                                    if (Objects.equals(estado.getNombre(), "Aprobado") || Objects.equals(estado.getNombre(), "Rechazado")) {
                                        return Mono.error(new SecurityValidationException(SolicitudMessage.SOLICITUD_PROCESADA.getMensaje()));
                                    }
                                    return Mono.just(solicitud);
                                })
                )
                .flatMap(solicitud -> {
                    if (aprobado) {
                        return aprobarSolicitud(solicitud);
                    } else {
                        return rechazarSolicitud(solicitud);
                    }
                })
                .flatMap(solicitudProcesada -> this.tipoPrestamoRepository.findById(solicitudProcesada.getIdTipoPrestamo())
                        .flatMap(tipoPrestamo -> {
                            sendSQSEventNotificaciones(solicitudProcesada, tipoPrestamo.getTasaInteres(), aprobado);
                            return Mono.just(solicitudProcesada);
                        }));

    }

    private Mono<Solicitud> aprobarSolicitud(Solicitud solicitud) {
        return this.estadoRepository.findByNombre("Aprobado")
                .flatMap(estadoAprobada -> {
                    solicitud.setIdEstado(estadoAprobada.getId());
                    return this.solicitudRepository.update(solicitud);
                });
    }

    private Mono<Solicitud> rechazarSolicitud(Solicitud solicitud) {
        return this.estadoRepository.findByNombre("Rechazado")
                .flatMap(estadoRechazada -> {
                    solicitud.setIdEstado(estadoRechazada.getId());
                    return this.solicitudRepository.update(solicitud);
                });
    }

    private void sendSQSEventNotificaciones(Solicitud solicitud, BigDecimal tasaInteres, Boolean aprobado) {
        // Crear evento
        SolicitudEventNotificaciones evento = SolicitudEventNotificaciones.builder()
                .idSolicitud(solicitud.getId())
                .aprobado(aprobado)
                .monto(solicitud.getMonto())
                .plazo(solicitud.getPlazo())
                .tasaInteres(tasaInteres)
                .email(solicitud.getEmail())
                .build();

        eventPublisherNotificaciones.publishEventAsync(evento);
    }

    private Mono<Void> sendSQSEventCapacidadEndeudamiento(Solicitud solicitud, BigDecimal tasaInteres, BigDecimal salarioBase ) {
        return this.calculateDeudaMensualActual(solicitud.getEmail())
                .map(deuda -> SolicitudEventCapacidadEndeudamiento.builder()
                            .idSolicitud(solicitud.getId())
                            .monto(solicitud.getMonto())
                            .plazo(solicitud.getPlazo())
                            .email(solicitud.getEmail())
                            .tasaInteres(tasaInteres)
                            .salario(salarioBase)
                            .deudaMensualActual(deuda)
                            .build()

        )
                .doOnNext(eventPublisherCapacidadEndeudamiento::publishEventAsync)
                .then();

    }


    // Metodo para procesar la solicitud entrante de la cola
    public Mono<Void> processSQSEventCapacidadEndeudamiento(SQSDTO sqsdto) {
        if (!Objects.equals(sqsdto.getEstado(), "Pendiente de revision")) {
            return this.solicitudRepository.findById(sqsdto.getIdSolicitud())
                    .zipWith(this.estadoRepository.findByNombre(sqsdto.getEstado()))
                    .flatMap(tuple -> {
                        Solicitud solicitud = tuple.getT1();
                        Estado estado = tuple.getT2();
                        solicitud.setIdEstado(estado.getId());
                        return this.solicitudRepository.update(solicitud);
                    })
                    .then();
        }
        return Mono.empty();
    }
}
