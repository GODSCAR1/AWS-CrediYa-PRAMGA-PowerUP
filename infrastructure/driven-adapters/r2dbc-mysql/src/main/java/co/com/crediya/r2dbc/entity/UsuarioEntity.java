package co.com.crediya.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.Date;

@Table("usuario")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UsuarioEntity {
    @Id
    @Column("id_usuario")
    private String id;
    private String nombre;
    private String apellido;
    @Column("email")
    private String email;
    private String contrasena;
    @Column("documento_identidad")
    private String documentoIdentidad;
    private String telefono;
    @Column("id_rol")
    private String idRol;
    @Column("salario_base")
    private BigDecimal salarioBase;
    @Column("fecha_nacimiento")
    private Date fechaNacimiento;

}
