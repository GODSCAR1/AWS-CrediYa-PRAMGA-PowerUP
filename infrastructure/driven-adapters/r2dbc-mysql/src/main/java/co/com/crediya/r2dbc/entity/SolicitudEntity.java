package co.com.crediya.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("solicitud")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SolicitudEntity {
    @Id
    @Column("id_solicitud")
    private String id;
    private BigDecimal monto;
    private Integer plazo;
    private String email;
    @Column("id_estado")
    private String idEstado;
    @Column("id_tipo_prestamo")
    private String idTipoPrestamo;
}
