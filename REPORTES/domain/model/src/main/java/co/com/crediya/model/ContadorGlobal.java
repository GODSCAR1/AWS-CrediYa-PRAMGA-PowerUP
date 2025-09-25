package co.com.crediya.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ContadorGlobal {

    private static final String GLOBAL_ID = "GLOBAL_COUNTERS";

    private String id;
    private Long cantidadPrestamosAprobados;
    private BigDecimal cantidadTotalPrestada;


    public static ContadorGlobal createInitial() {
        return new ContadorGlobal(GLOBAL_ID, 0L, BigDecimal.ZERO);
    }

}
