package co.com.crediya.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUsuarioDtoRequest{
    String nombre;
    String apellido;
    String email;
    String contrasena;
    String documentoIdentidad;
    String telefono;
    BigDecimal salarioBase;
    Date fechaNacimiento;
}
