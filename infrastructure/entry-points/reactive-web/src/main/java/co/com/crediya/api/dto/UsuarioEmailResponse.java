package co.com.crediya.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioEmailResponse {
    private String nombre;
    private String email;
    private BigDecimal salarioBase;
}
