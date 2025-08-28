package co.com.crediya.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Usuario {
    private String id;
    private String nombre;
    private String apellido;
    private String email;
    private String contrasena;
    private String documentoIdentidad;
    private String telefono;
    private String idRol;
    private BigDecimal salarioBase;
    private Date fechaNacimiento;
}
