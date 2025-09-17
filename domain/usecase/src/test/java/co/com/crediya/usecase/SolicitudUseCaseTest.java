package co.com.crediya.usecase;

import co.com.crediya.model.dto.SQSDTO;
import co.com.crediya.model.estados.Estado;
import co.com.crediya.model.estados.gateways.EstadoRepository;
import co.com.crediya.model.events.SolicitudEventCapacidadEndeudamiento;
import co.com.crediya.model.events.SolicitudEventNotificaciones;
import co.com.crediya.model.events.gateways.EventPublisherCapacidadEndeudamiento;
import co.com.crediya.model.events.gateways.EventPublisherNotificaciones;
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
import co.com.crediya.usecase.exception.SolicitudValidationException;
import co.com.crediya.usecase.message.SolicitudMessage;
import co.com.crediya.usecase.message.ValidationMessage;
import co.com.crediya.usecase.validation.MontoValidator;
import co.com.crediya.usecase.validation.PlazoValidator;
import co.com.crediya.usecase.validation.PrestamoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SolicitudUseCaseTest {

    private SolicitudUseCase solicitudUseCase;
    @Mock
    private SolicitudRepository solicitudRepository;
    @Mock
    private EstadoRepository estadoRepository;
    @Mock
    private TipoPrestamoRepository tipoPrestamoRepository;

    @Mock
    private CustomSolicitudRepository customSolicitudRepository;
    @Mock
    private UsuarioConsumer usuarioConsumer;

    @Mock
    private EventPublisherNotificaciones eventPublisherNotificaciones;

    @Mock
    private EventPublisherCapacidadEndeudamiento eventPublisherCapacidadEndeudamiento;

    private Solicitud solicitud;

    private String authHeader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SolicitudValidationComposite solicitudValidationComposite =
                this.getSolicitudValidationComposite();

        // Crear el UseCase con todas sus dependencias
        solicitudUseCase =
                new SolicitudUseCase(
                        solicitudValidationComposite,
                        solicitudRepository,
                        estadoRepository,
                        tipoPrestamoRepository,
                        customSolicitudRepository,
                        usuarioConsumer,
                        eventPublisherNotificaciones,
                        eventPublisherCapacidadEndeudamiento);

        solicitud = Solicitud.builder()
                .email("oscar@gmail.com")
                .plazo(12)
                .monto( BigDecimal.valueOf(400000) )
                .nombreTipoPrestamo("Microcredito")
                .build();
        authHeader = "oscar@gmail.com";
    }

    private SolicitudValidationComposite getSolicitudValidationComposite() {
        MontoValidator montoValidator = new MontoValidator(tipoPrestamoRepository);
        PlazoValidator plazoValidator = new PlazoValidator();
        PrestamoValidator prestamoValidator = new PrestamoValidator(tipoPrestamoRepository);

        return new SolicitudValidationComposite(
                montoValidator,
                plazoValidator,
                prestamoValidator
        );
    }

    @Test
    void mustFailWhenMontoisNull(){
        solicitud.setMonto(null);
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals(ValidationMessage.MONTO_OBLIGATORIO.getMensaje()))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenMontoisLessOrEqualToZero(){
        solicitud.setMonto(BigDecimal.valueOf(0));
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals(ValidationMessage.MONTO_DEBE_SER_MAYOR_A_CERO.getMensaje()))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenPlazoIsNull(){
        solicitud.setPlazo(null);
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals(ValidationMessage.PLAZO_DEBE_SER_MAYOR_A_CERO.getMensaje()))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenPlazoIsLessOrEqualToZero(){
        solicitud.setPlazo(0);
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud,authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals(ValidationMessage.PLAZO_DEBE_SER_MAYOR_A_CERO.getMensaje()))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenPrestamoIsNull(){
        solicitud.setNombreTipoPrestamo(null);
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud,authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals(ValidationMessage.NOMBRE_TIPO_PRESTAMO_OBLIGATORIO.getMensaje()))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenPrestamoIsBlank(){
        solicitud.setNombreTipoPrestamo("");
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud,authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals(ValidationMessage.NOMBRE_TIPO_PRESTAMO_OBLIGATORIO.getMensaje()))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }
    @Test
    void mustFailWhenPrestamoIsNotFound(){
        when(tipoPrestamoRepository.findByNombre(anyString()))
                .thenReturn(Mono.empty());
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud,authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals(ValidationMessage.TIPO_PRESTAMO_NO_ENCONTRADO.getMensaje()))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenPrestamoIsNotInProperRange(){
        TipoPrestamo tipoPrestamo = TipoPrestamo.builder()
                .id("1")
                .nombre("Microcredito")
                .montoMinimo(BigDecimal.valueOf(100000))
                .montoMaximo(BigDecimal.valueOf(300000))
                .build();
        when(tipoPrestamoRepository.findByNombre(anyString()))
                .thenReturn(Mono.just(
                        tipoPrestamo
                ));
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals(String.format(ValidationMessage.MONTO_DEBE_ESTAR_ENTRE.getMensaje(),
                                tipoPrestamo.getMontoMinimo(), tipoPrestamo.getMontoMaximo())))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenEstadoIsNotFound(){
        TipoPrestamo tipoPrestamo = TipoPrestamo.builder()
                .id("1")
                .nombre("Microcredito")
                .montoMinimo(BigDecimal.valueOf(100000))
                .montoMaximo(BigDecimal.valueOf(500000))
                .build();
        when(tipoPrestamoRepository.findByNombre(anyString()))
                .thenReturn(Mono.just(
                        tipoPrestamo
                ));
        when(estadoRepository.findByNombre(anyString()))
                .thenReturn(Mono.empty());
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof EstadoValidationException
                        && ex.getMessage().equals(SolicitudMessage.ESTADO_PENDIENTE_REVISION_NO_ENCONTRADO.getMensaje()))
                .verify();
        verify(estadoRepository).findByNombre(anyString());
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustSucceedWhenAllDataIsCorrectAndHasAutomaticValidation(){
        TipoPrestamo tipoPrestamo = TipoPrestamo.builder()
                .id("1")
                .nombre("Microcredito")
                .montoMinimo(BigDecimal.valueOf(100000))
                .montoMaximo(BigDecimal.valueOf(500000))
                .tasaInteres(BigDecimal.valueOf(1))
                .validacionAutomatica(Boolean.TRUE)
                .build();
        when(tipoPrestamoRepository.findByNombre(anyString()))
                .thenReturn(Mono.just(
                        tipoPrestamo
                ));
        when(estadoRepository.findByNombre(anyString()))
                .thenReturn(Mono.just(
                        Estado.builder()
                                .id("1")
                                .nombre("Pendiente de revision")
                                .build()
                ));
        when(solicitudRepository.save(solicitud))
                .thenAnswer(invocation -> {
                    Solicitud s = invocation.getArgument(0);
                    s.setId("1");
                    return Mono.just(s);
                });
        when(usuarioConsumer.getUsuariosByEmails(List.of(solicitud.getEmail()))).thenReturn(
                Flux.just(
                        Usuario.builder()
                                .email(solicitud.getEmail())
                                .nombre("Oscar")
                                .salarioBase(BigDecimal.valueOf(3000000))
                                .build()
                )
        );
        when(solicitudRepository.findByEmailAndEstadoNombre(anyString(), anyString()))
                .thenReturn(Flux.just(
                        Solicitud.builder()
                                .id("1")
                                .email(solicitud.getEmail())
                                .idEstado("1")
                                .nombreTipoPrestamo("Microcredito")
                                .idTipoPrestamo("1")
                                .monto(BigDecimal.valueOf(200000))
                                .plazo(12)
                                .build()));

        when(tipoPrestamoRepository.findById(anyString()))
                .thenReturn(Mono.just(tipoPrestamo));

        doNothing().when(eventPublisherCapacidadEndeudamiento).publishEventAsync(any(SolicitudEventCapacidadEndeudamiento.class));
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectNextMatches(s -> s.getId() != null && !s.getId().isEmpty()
                        && s.getIdEstado().equals("1")
                        && s.getIdTipoPrestamo().equals("1")
                        && s.getNombreTipoPrestamo().equals("Microcredito"))
                .verifyComplete();
        verify(estadoRepository).findByNombre(anyString());
        verify(solicitudRepository).save(solicitud);
        verify(eventPublisherCapacidadEndeudamiento).publishEventAsync(any(SolicitudEventCapacidadEndeudamiento.class));
    }

    @Test
    void mustSucceedWhenAllDataIsCorrectAndDontHaveAutomaticValidation(){
        TipoPrestamo tipoPrestamo = TipoPrestamo.builder()
                .id("1")
                .nombre("Microcredito")
                .montoMinimo(BigDecimal.valueOf(100000))
                .montoMaximo(BigDecimal.valueOf(500000))
                .tasaInteres(BigDecimal.valueOf(1))
                .validacionAutomatica(Boolean.FALSE)
                .build();
        when(tipoPrestamoRepository.findByNombre(anyString()))
                .thenReturn(Mono.just(
                        tipoPrestamo
                ));
        when(estadoRepository.findByNombre(anyString()))
                .thenReturn(Mono.just(
                        Estado.builder()
                                .id("1")
                                .nombre("Pendiente de revision")
                                .build()
                ));
        when(solicitudRepository.save(solicitud))
                .thenAnswer(invocation -> {
                    Solicitud s = invocation.getArgument(0);
                    s.setId("1");
                    return Mono.just(s);
                });

        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectNextMatches(s -> s.getId() != null && !s.getId().isEmpty()
                        && s.getIdEstado().equals("1")
                        && s.getIdTipoPrestamo().equals("1")
                        && s.getNombreTipoPrestamo().equals("Microcredito"))
                .verifyComplete();
        verify(estadoRepository).findByNombre(anyString());
        verify(solicitudRepository).save(solicitud);
        verify(usuarioConsumer, never()).getUsuariosByEmails(List.of(solicitud.getEmail()));
    }

    @Test
    void testGetSolicitudPagedMustSuccessWithMultipleSolicitudesAndUsers() {
        // Arrange
        String nombreEstado = "Pendiente de revision";
        int page = 0;
        int size = 10;
        String sortBy = "monto";
        String sortDirection = "DESC";
        Long totalCount = 3L;

        // Crear solicitudes de prueba (algunas con el mismo email)
        List<SolicitudInfo> mockSolicitudes = Arrays.asList(
                SolicitudInfo.builder()
                        .email("juan@email.com")
                        .monto(new BigDecimal("1000000"))
                        .build(),
                SolicitudInfo.builder()
                        .email("juan@email.com")
                        .monto(new BigDecimal("500000"))
                        .build(),
                SolicitudInfo.builder()
                        .email("maria@email.com")
                        .monto(new BigDecimal("2000000"))
                        .build()
        );

        // Crear usuarios de prueba
        List<Usuario> mockUsuarios = Arrays.asList(
                Usuario.builder()
                        .email("juan@email.com")
                        .nombre("Juan")
                        .salarioBase(new BigDecimal("3000000"))
                        .build(),
                Usuario.builder()
                        .email("maria@email.com")
                        .nombre("María")
                        .salarioBase(new BigDecimal("5000000"))
                        .build()
        );

        List<String> expectedEmails = Arrays.asList("juan@email.com", "maria@email.com");

        // Mock de los repositorios
        when(customSolicitudRepository.countByNombreEstado(nombreEstado))
                .thenReturn(Mono.just(totalCount));
        when(customSolicitudRepository.findByAllByEstado(nombreEstado, page, size, sortBy, sortDirection))
                .thenReturn(Flux.fromIterable(mockSolicitudes));
        when(usuarioConsumer.getUsuariosByEmails(anyList()))
                .thenReturn(Flux.fromIterable(mockUsuarios));

        when(solicitudRepository.findByEmailAndEstadoNombre(anyString(),anyString())).thenReturn(Flux.empty());
        when(tipoPrestamoRepository.findById(anyString())).thenReturn(Mono.just(TipoPrestamo.builder().tasaInteres(BigDecimal.ONE).build()));

        // Act & Assert
        StepVerifier.create(solicitudUseCase.getSolicitudPaged(nombreEstado, page, size, sortBy, sortDirection))
                .expectNextMatches(result -> {
                    // Verificar estructura de PagedSolicitud
                    assertEquals(3, result.getContent().size());
                    assertEquals(page, result.getPageNumber());
                    assertEquals(size, result.getPageSize());
                    assertEquals(totalCount, result.getTotalElements());
                    assertEquals(1, result.getTotalPages());
                    assertTrue(result.isFirst());
                    assertTrue(result.isLast());

                    // Verificar que las solicitudes están enriquecidas
                    SolicitudInfo solicitud1 = result.getContent().get(0);
                    assertNotNull(solicitud1.getNombre());
                    assertEquals("Juan", solicitud1.getNombre());
                    assertEquals("juan@email.com", solicitud1.getEmail());
                    assertEquals(new BigDecimal("3000000"), solicitud1.getSalarioBase());
                    assertNotNull(solicitud1.getDeudaTotalMensual());

                    SolicitudInfo solicitud2 = result.getContent().get(1);
                    assertNotNull(solicitud2.getNombre());
                    assertEquals("Juan", solicitud2.getNombre());
                    assertEquals("juan@email.com", solicitud2.getEmail());
                    assertEquals(new BigDecimal("3000000"), solicitud1.getSalarioBase());
                    assertNotNull(solicitud2.getDeudaTotalMensual());

                    SolicitudInfo solicitud3 = result.getContent().get(2);
                    assertNotNull(solicitud3.getNombre());
                    assertEquals("María", solicitud3.getNombre());
                    assertEquals(new BigDecimal("5000000"), solicitud3.getSalarioBase());
                    assertEquals("maria@email.com", solicitud3.getEmail());
                    assertNotNull(solicitud3.getDeudaTotalMensual());

                    return true;
                })
                .verifyComplete();
    }

    @Test
    void testGetSolicitudPagedMustSuccessWithNoSolicitudes() {
        String nombreEstado = "Pendiente de revision";
        int page = 0;
        int size = 10;
        String sortBy = "monto";
        String sortDirection = "DESC";
        Long totalCount = 0L;

        when(customSolicitudRepository.countByNombreEstado(nombreEstado))
                .thenReturn(Mono.just(0L));
        when(customSolicitudRepository.findByAllByEstado(nombreEstado, page, size, sortBy, sortDirection))
                .thenReturn(Flux.empty());

        StepVerifier.create(solicitudUseCase.getSolicitudPaged(nombreEstado, page, size, sortBy, sortDirection))
                .expectNextMatches(result -> {
                    // Verificar estructura de PagedSolicitud
                    assertEquals(0, result.getContent().size());
                    assertEquals(page, result.getPageNumber());
                    assertEquals(size, result.getPageSize());
                    assertEquals(totalCount, result.getTotalElements());
                    assertEquals(0, result.getTotalPages());
                    assertTrue(result.isFirst());
                    assertTrue(result.isLast());
                    return true;
                }).verifyComplete();
    }


    @Test
    void testHandleSolicitudManualMustFailWhenSolicitudIsNotFound() {
        String id = "1";
        Boolean aprobado = Boolean.TRUE;

        when(solicitudRepository.findById(id))
                .thenReturn(Mono.empty());
        StepVerifier.create(solicitudUseCase.handleSolicitudManual(id, aprobado))
                .expectErrorMatches(ex -> ex instanceof SolicitudNotFoundException
                        && ex.getMessage().equals(String.format(SolicitudMessage.SOLICITUD_NO_ENCONTRADA.getMensaje(), id)))
                .verify();
    }

    @Test
    void testHandleSolicitudManualMustFailWhenSolicitudIsAlreadyProcessed() {
        String id = "1";
        Boolean aprobado = Boolean.TRUE;

        Solicitud existingSolicitud = Solicitud.builder()
                .id(id)
                .idEstado("2") // Estado diferente a "Pendiente de revision"
                .build();

        when(solicitudRepository.findById(id))
                .thenReturn(Mono.just(existingSolicitud));
        when(estadoRepository.findById("2")).thenReturn(Mono.just(Estado.builder().id("2").nombre("Aprobado").build()));

        StepVerifier.create(solicitudUseCase.handleSolicitudManual(id, aprobado))
                .expectErrorMatches(ex -> ex instanceof SecurityValidationException
                        && ex.getMessage().equals(SolicitudMessage.SOLICITUD_PROCESADA.getMensaje()))
                .verify();
    }

    @Test
    void testHandleSolicitudManualShouldApproveSuccessfully() {
        String id = "1";
        Boolean aprobado = Boolean.TRUE;

        Solicitud existingSolicitud = Solicitud.builder()
                .id(id)
                .idEstado("1") // Estado "Pendiente de revision"
                .build();

        Estado estadoPendiente = Estado.builder()
                .id("1")
                .nombre("Pendiente de revision")
                .build();

        Estado estadoAprobado = Estado.builder()
                .id("2")
                .nombre("Aprobado")
                .build();

        Solicitud updatedSolicitud = Solicitud.builder()
                .id(id)
                .idEstado("2")
                .idTipoPrestamo("1")// Estado cambiado a "Aprobado"
                .build();

        when(solicitudRepository.findById(id))
                .thenReturn(Mono.just(existingSolicitud));
        when(estadoRepository.findById("1"))
                .thenReturn(Mono.just(estadoPendiente));
        when(estadoRepository.findByNombre("Aprobado"))
                .thenReturn(Mono.just(estadoAprobado));

        when(tipoPrestamoRepository.findById(anyString())).thenReturn(Mono.just(TipoPrestamo.builder().tasaInteres(BigDecimal.ONE).build()));

        doNothing().when(eventPublisherNotificaciones).publishEventAsync(any(SolicitudEventNotificaciones.class));

        when(solicitudRepository.update(any(Solicitud.class)))
                .thenReturn(Mono.just(updatedSolicitud));

        StepVerifier.create(solicitudUseCase.handleSolicitudManual(id, aprobado))
                .expectNextMatches(s -> s.getId().equals(id) && s.getIdEstado().equals("2"))
                .verifyComplete();

        verify(eventPublisherNotificaciones).publishEventAsync(any(SolicitudEventNotificaciones.class));
        verify(solicitudRepository).update(any(Solicitud.class));
    }

    @Test
    void testHandleSolicitudManualShouldRejectSuccessfully() {
        String id = "1";
        Boolean aprobado = Boolean.FALSE;

        Solicitud existingSolicitud = Solicitud.builder()
                .id(id)
                .idEstado("1")
                .idTipoPrestamo("1")// Estado "Pendiente de revision"
                .build();

        Estado estadoPendiente = Estado.builder()
                .id("1")
                .nombre("Pendiente de revision")
                .build();

        Estado estadoRechazado = Estado.builder()
                .id("2")
                .nombre("Rechazado")
                .build();

        Solicitud updatedSolicitud = Solicitud.builder()
                .id(id)
                .idEstado("2")
                .idTipoPrestamo("1")// Estado cambiado a "Rechazado"
                .build();

        when(solicitudRepository.findById(id))
                .thenReturn(Mono.just(existingSolicitud));
        when(estadoRepository.findById("1"))
                .thenReturn(Mono.just(estadoPendiente));
        when(estadoRepository.findByNombre("Rechazado"))
                .thenReturn(Mono.just(estadoRechazado));
        doNothing().when(eventPublisherNotificaciones).publishEventAsync(any(SolicitudEventNotificaciones.class));
        when(tipoPrestamoRepository.findById(anyString())).thenReturn(Mono.just(TipoPrestamo.builder().tasaInteres(BigDecimal.ONE).build()));
        when(solicitudRepository.update(any(Solicitud.class)))
                .thenReturn(Mono.just(updatedSolicitud));

        StepVerifier.create(solicitudUseCase.handleSolicitudManual(id, aprobado))
                .expectNextMatches(s -> s.getId().equals(id) && s.getIdEstado().equals("2"))
                .verifyComplete();

        verify(eventPublisherNotificaciones).publishEventAsync(any(SolicitudEventNotificaciones.class));
        verify(solicitudRepository).update(any(Solicitud.class));
    }

    @Test
    void testProcessSQSEventCapacidadEndeudamientoMustUpdateWhenEstadoIsNotPendienteDeRevision() {
        SQSDTO sqsdto = SQSDTO.builder()
                .idSolicitud("1")
                .estado("Aprobado")
                .build();
        Solicitud existingSolicitud = Solicitud.builder()
                .id("1")
                .idEstado("1")
                .build();
        Estado estadoAprobado = Estado.builder()
                .id("2")
                .nombre("Aprobado")
                .build();

        when(solicitudRepository.findById("1"))
                .thenReturn(Mono.just(existingSolicitud));
        when(estadoRepository.findByNombre(sqsdto.getEstado())).thenReturn(Mono.just(estadoAprobado));

        when(solicitudRepository.update(any(Solicitud.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(solicitudUseCase.processSQSEventCapacidadEndeudamiento(sqsdto))
                .verifyComplete();

        verify(solicitudRepository).update(any(Solicitud.class));
    }

    @Test
    void testProcessSQSEventCapacidadEndeudamientoMustNotUpdateWhenEstadoIsPendienteDeRevision() {
        SQSDTO sqsdto = SQSDTO.builder()
                .idSolicitud("1")
                .estado("Pendiente de revision")
                .build();

        StepVerifier.create(solicitudUseCase.processSQSEventCapacidadEndeudamiento(sqsdto))
                .verifyComplete();

        verify(solicitudRepository, never()).update(any(Solicitud.class));
    }

}
