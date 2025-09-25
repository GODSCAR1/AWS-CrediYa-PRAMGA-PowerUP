package co.com.crediya.model.events;

import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReporteDiarioEvent {
    private Long cantidadPrestamosAprobados;
    private BigDecimal cantidadTotalPrestada;
}
