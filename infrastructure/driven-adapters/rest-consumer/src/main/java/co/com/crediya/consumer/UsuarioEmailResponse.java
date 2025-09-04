package co.com.crediya.consumer;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class UsuarioEmailResponse {

    private String nombre;
    private String email;
    private BigDecimal salarioBase;

}