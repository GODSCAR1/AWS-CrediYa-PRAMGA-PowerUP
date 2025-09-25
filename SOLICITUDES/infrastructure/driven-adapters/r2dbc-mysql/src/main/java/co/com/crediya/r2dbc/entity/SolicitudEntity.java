package co.com.crediya.r2dbc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("solicitud")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SolicitudEntity implements Persistable<String> {
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

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return this.isNew;
    }
    public void markNotNew() {
        this.isNew = false;
    }
}
