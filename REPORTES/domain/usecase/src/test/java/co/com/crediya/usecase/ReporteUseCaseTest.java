package co.com.crediya.usecase;

import co.com.crediya.model.ContadorGlobal;
import co.com.crediya.model.dto.SQSDTO;
import co.com.crediya.model.events.gateways.EventPublisherReporteDiario;
import co.com.crediya.model.gateways.ContadorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


class ReporteUseCaseTest {
    private ReporteUseCase reporteUseCase;

    @Mock
    private ContadorRepository contadorRepository;

    @Mock
    private EventPublisherReporteDiario eventPublisherReporteDiario;

    private ContadorGlobal contadorGlobal;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);

        reporteUseCase = new ReporteUseCase(contadorRepository, eventPublisherReporteDiario);

        contadorGlobal = ContadorGlobal.builder()
                .cantidadPrestamosAprobados(10L)
                .cantidadTotalPrestada(BigDecimal.valueOf(5000.0))
                .build();
    }

    @Test
    void testGetReporte(){
        when(contadorRepository.getCounters()).thenReturn(Mono.just(contadorGlobal));
        StepVerifier.create(reporteUseCase.getReporte()).expectNext(contadorGlobal).verifyComplete();
    }

    @Test
    void testProcessSQSEventReportes(){
        ContadorGlobal updatedContador = contadorGlobal.incrementLoan(BigDecimal.valueOf(1000.0));
        when(contadorRepository.incrementCounters(BigDecimal.valueOf(1000.0))).thenReturn(Mono.just(updatedContador));
        StepVerifier.create(reporteUseCase.processSQSEventReportes(SQSDTO.builder().monto(BigDecimal.valueOf(1000.0)).build())).verifyComplete();
    }

    @Test
    void testSendSQSEventReportesDiarios(){
        doNothing().when(eventPublisherReporteDiario).publishEventAsync(any());
        when(contadorRepository.getCounters()).thenReturn(Mono.just(contadorGlobal));
        StepVerifier.create(reporteUseCase.sendSQSEventReportesDiarios()).verifyComplete();
    }
}
