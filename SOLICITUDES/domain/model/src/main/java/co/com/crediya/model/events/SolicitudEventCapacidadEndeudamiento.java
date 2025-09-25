package co.com.crediya.model.events;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SolicitudEventCapacidadEndeudamiento {
    String email;
    String idSolicitud;
    BigDecimal deudaMensualActual;
    BigDecimal monto;
    BigDecimal tasaInteres;
    Integer plazo;
    BigDecimal salario;
}
