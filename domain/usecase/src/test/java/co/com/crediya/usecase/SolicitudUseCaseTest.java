package co.com.crediya.usecase;

import co.com.crediya.model.estados.Estado;
import co.com.crediya.model.estados.gateways.EstadoRepository;
import co.com.crediya.model.solicitud.Solicitud;
import co.com.crediya.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya.model.tipoprestamo.TipoPrestamo;
import co.com.crediya.model.tipoprestamo.gateways.TipoPrestamoRepository;
import co.com.crediya.usecase.composite.SolicitudValidationComposite;
import co.com.crediya.usecase.exception.EstadoValidationException;
import co.com.crediya.usecase.exception.SecurityValidationException;
import co.com.crediya.usecase.exception.SolicitudValidationException;
import co.com.crediya.usecase.validation.EmailValidator;
import co.com.crediya.usecase.validation.MontoValidator;
import co.com.crediya.usecase.validation.PlazoValidator;
import co.com.crediya.usecase.validation.PrestamoValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

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
                        tipoPrestamoRepository);

        solicitud = Solicitud.builder()
                .email("oscar@gmail.com")
                .plazo(12)
                .monto( BigDecimal.valueOf(400000) )
                .nombreTipoPrestamo("Microcredito")
                .build();
        authHeader = "oscar@gmail.com";
    }

    private SolicitudValidationComposite getSolicitudValidationComposite() {
        EmailValidator emailValidator = new EmailValidator();
        MontoValidator montoValidator = new MontoValidator(tipoPrestamoRepository);
        PlazoValidator plazoValidator = new PlazoValidator();
        PrestamoValidator prestamoValidator = new PrestamoValidator(tipoPrestamoRepository);

        return new SolicitudValidationComposite(
                montoValidator,
                plazoValidator,
                prestamoValidator,
                emailValidator
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
    void mustFailWhenEmailIsNull(){
        solicitud.setEmail(null);
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
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals("El email es obligatorio"))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }



    @Test
    void mustFailWhenEmailIsBlank(){
        solicitud.setEmail("");
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
        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof SolicitudValidationException
                        && ex.getMessage().equals("El email es obligatorio"))
                .verify();
        verify(solicitudRepository, never()).save(solicitud);
    }

    @Test
    void mustFailWhenEmailIsNotValid(){
        authHeader = "oscar31@gmail.com";
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

        StepVerifier.create(solicitudUseCase.createSolicitud(solicitud, authHeader))
                .expectErrorMatches(ex -> ex instanceof SecurityValidationException
                        && ex.getMessage().equals("El email de la solicitud no coincide con el email del usuario autenticado"))
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




}
