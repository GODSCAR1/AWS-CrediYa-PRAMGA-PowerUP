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
public class SolicitudInfoDTO {
    private String id;

    private BigDecimal monto;

    private Integer plazo;

    private String email;

    private String nombre;

    private String nombreTipoPrestamo;

    private BigDecimal tasaInteres;

    private String nombreEstado;

    private BigDecimal salarioBase;
}
