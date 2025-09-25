package co.com.crediya.model.events;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SolicitudEventNotificaciones {
    BigDecimal monto;
    Integer plazo;
    BigDecimal tasaInteres;
    String idSolicitud;
    String email;
    Boolean aprobado;
}
