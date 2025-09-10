package co.com.crediya.model.events;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SolicitudEvent {
    BigDecimal monto;
    Integer plazo;
    String idSolicitud;
    String email;
    Boolean aprobado;
}
