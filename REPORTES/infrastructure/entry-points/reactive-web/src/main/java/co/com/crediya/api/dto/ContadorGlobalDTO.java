package co.com.crediya.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContadorGlobalDTO {
    private Long cantidadPrestamosAprobados;
    private BigDecimal cantidadTotalPrestada;
}
