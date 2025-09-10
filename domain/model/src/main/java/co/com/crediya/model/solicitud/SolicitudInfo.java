package co.com.crediya.model.solicitud;

import lombok.*;

import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SolicitudInfo {
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
