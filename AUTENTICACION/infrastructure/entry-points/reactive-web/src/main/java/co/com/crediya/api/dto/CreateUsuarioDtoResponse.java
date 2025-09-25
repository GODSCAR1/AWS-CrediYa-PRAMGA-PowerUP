package co.com.crediya.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUsuarioDtoResponse{
    String nombre;
    String apellido;
    String email;
    String documentoIdentidad;
    String telefono;
    String idRol;
    BigDecimal salarioBase;
    Date fechaNacimiento;
}
