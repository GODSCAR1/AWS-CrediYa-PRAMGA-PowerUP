package co.com.crediya.model.solicitud;
import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Solicitud {
    private String id;
    private BigDecimal monto;
    private Integer plazo;
    private String email;
    private String nombreTipoPrestamo;
    private String idEstado;
    private String idTipoPrestamo;
}
