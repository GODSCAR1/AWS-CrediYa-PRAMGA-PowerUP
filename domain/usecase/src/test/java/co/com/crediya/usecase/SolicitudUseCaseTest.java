package co.com.crediya.usecase;

import co.com.crediya.model.estados.Estado;
import co.com.crediya.model.estados.gateways.EstadoRepository;
import co.com.crediya.model.events.SolicitudEvent;
import co.com.crediya.model.events.gateways.EventPublisher;
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
    private EventPublisher eventPublisher;

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
                        eventPublisher);

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
                        && ex.getMessage().equals("El monto es obligatorio"))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenMontoisLessOrEqualToZero(){
        solicitud.setMonto(BigDecimal.valueOf(0));
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals("El monto debe ser mayor a cero"))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenPlazoIsNull(){
        solicitud.setPlazo(null);
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals("El plazo debe ser mayor a 0"))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenPlazoIsLessOrEqualToZero(){
        solicitud.setPlazo(0);
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud,authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals("El plazo debe ser mayor a 0"))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenPrestamoIsNull(){
        solicitud.setNombreTipoPrestamo(null);
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud,authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals("El nombre del tipo de préstamo no puede ser nulo o vacio"))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenPrestamoIsBlank(){
        solicitud.setNombreTipoPrestamo("");
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud,authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals("El nombre del tipo de préstamo no puede ser nulo o vacio"))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }
    @Test
    void mustFailWhenPrestamoIsNotFound(){
        when(tipoPrestamoRepository.findByNombre(anyString()))
                .thenReturn(Mono.empty());
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud,authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals("El tipo de préstamo no existe"))
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
                        && ex.getMessage().equals("El monto debe estar entre "
                        + tipoPrestamo.getMontoMinimo() + " y " + tipoPrestamo.getMontoMaximo()))
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
                        && ex.getMessage().equals("El estado 'Pendiente de revision' no existe"))
                .verify();
        verify(estadoRepository).findByNombre(anyString());
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustSucceedWhenAllDataIsCorrect(){
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
                .expectNextMatches(s -> s.getId() != null && s.getId().equals("1")
                        && s.getIdEstado().equals("1")
                        && s.getIdTipoPrestamo().equals("1")
                        && s.getNombreTipoPrestamo().equals("Microcredito"))
                .verifyComplete();
        verify(estadoRepository).findByNombre(anyString());
        verify(solicitudRepository).save(solicitud);
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

                    SolicitudInfo solicitud2 = result.getContent().get(1);
                    assertNotNull(solicitud2.getNombre());
                    assertEquals("Juan", solicitud2.getNombre());
                    assertEquals("juan@email.com", solicitud2.getEmail());
                    assertEquals(new BigDecimal("3000000"), solicitud1.getSalarioBase());

                    SolicitudInfo solicitud3 = result.getContent().get(2);
                    assertNotNull(solicitud3.getNombre());
                    assertEquals("María", solicitud3.getNombre());
                    assertEquals(new BigDecimal("5000000"), solicitud3.getSalarioBase());
                    assertEquals("maria@email.com", solicitud3.getEmail());

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
                        && ex.getMessage().equals("No se encontró la solicitud con ID: " + id))
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
                        && ex.getMessage().equals("La solicitud ya ha sido procesada y no puede ser modificada."))
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
                .idEstado("2") // Estado cambiado a "Aprobado"
                .build();

        when(solicitudRepository.findById(id))
                .thenReturn(Mono.just(existingSolicitud));
        when(estadoRepository.findById("1"))
                .thenReturn(Mono.just(estadoPendiente));
        when(estadoRepository.findByNombre("Aprobado"))
                .thenReturn(Mono.just(estadoAprobado));
        doNothing().when(eventPublisher).publishEventAsync(any(SolicitudEvent.class));
        when(solicitudRepository.save(any(Solicitud.class)))
                .thenReturn(Mono.just(updatedSolicitud));

        StepVerifier.create(solicitudUseCase.handleSolicitudManual(id, aprobado))
                .expectNextMatches(s -> s.getId().equals(id) && s.getIdEstado().equals("2"))
                .verifyComplete();

        verify(solicitudRepository).save(any(Solicitud.class));
    }

    @Test
    void testHandleSolicitudManualShouldRejectSuccessfully() {
        String id = "1";
        Boolean aprobado = Boolean.FALSE;

        Solicitud existingSolicitud = Solicitud.builder()
                .id(id)
                .idEstado("1") // Estado "Pendiente de revision"
                .build();

        Estado estadoPendiente = Estado.builder()
                .id("1")
                .nombre("Pendiente de revision")
                .build();

        Estado estadoAprobado = Estado.builder()
                .id("1")
                .nombre("Rechazado")
                .build();

        Solicitud updatedSolicitud = Solicitud.builder()
                .id(id)
                .idEstado("1") // Estado cambiado a "Rechazado"
                .build();

        when(solicitudRepository.findById(id))
                .thenReturn(Mono.just(existingSolicitud));
        when(estadoRepository.findById("1"))
                .thenReturn(Mono.just(estadoPendiente));
        when(estadoRepository.findByNombre("Rechazado"))
                .thenReturn(Mono.just(estadoAprobado));
        doNothing().when(eventPublisher).publishEventAsync(any(SolicitudEvent.class));
        when(solicitudRepository.save(any(Solicitud.class)))
                .thenReturn(Mono.just(updatedSolicitud));

        StepVerifier.create(solicitudUseCase.handleSolicitudManual(id, aprobado))
                .expectNextMatches(s -> s.getId().equals(id) && s.getIdEstado().equals("1"))
                .verifyComplete();

        verify(solicitudRepository).save(any(Solicitud.class));
    }


}
