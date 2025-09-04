package co.com.crediya.model.usuario;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Usuario {
    private String nombre;
    private String email;
    private BigDecimal salarioBase;
}
