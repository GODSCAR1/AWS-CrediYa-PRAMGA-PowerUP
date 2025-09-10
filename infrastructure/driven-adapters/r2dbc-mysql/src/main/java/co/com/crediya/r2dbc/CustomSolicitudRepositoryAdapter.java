package co.com.crediya.r2dbc;


import co.com.crediya.model.solicitud.SolicitudInfo;
import co.com.crediya.model.solicitud.gateways.CustomSolicitudRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
@Log
public class CustomSolicitudRepositoryAdapter implements CustomSolicitudRepository {
    private final DatabaseClient databaseClient;
    @Override
    public Flux<SolicitudInfo> findByAllByEstado(String nombreEstado, int page, int size, String sortBy, String sortDirection) {
        String sql = buildQuery(sortBy, sortDirection);
        int offset = page * size;

        log.info("Executing SQL: " + sql + " with nombreEstado=" + nombreEstado + ", size=" + size + ", offset=" + offset);

        return databaseClient.sql(sql)
                .bind("nombreEstado", "%" + nombreEstado + "%")
                .bind("size", size)
                .bind("offset", offset)
                .map((row, metadata) -> SolicitudInfo.builder()
                        .id(row.get("id_solicitud", String.class))
                        .monto(row.get("monto", BigDecimal.class))
                        .plazo(row.get("plazo", Integer.class))
                        .email(row.get("email", String.class))
                        .nombreTipoPrestamo(row.get("nombre_tipo_prestamo", String.class))
                        .tasaInteres(row.get("tasa_interes", BigDecimal.class))
                        .nombreEstado(row.get("nombre_estado", String.class))
                        .build())
                .all();
    }

    public Mono<Long> countByNombreEstado(String nombreEstado) {
        String query = """
            SELECT COUNT(s.id_solicitud)
            FROM solicitud s 
            JOIN estados e ON e.id_estado = e.id_estado
            WHERE UPPER(e.nombre) LIKE UPPER(:nombre)
            """;

        return databaseClient.sql(query)
                .bind("nombre", "%" + nombreEstado + "%")
                .map(row -> row.get(0, Long.class))
                .one();

    }

    private String buildQuery(String sortBy, String sortDirection) {
       String orderByColumn = getOrderByColumn(sortBy);
       String direction = "desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
       return """
               SELECT s.id_solicitud, s.monto, s.plazo, s.email, t.nombre AS nombre_tipo_prestamo, t.tasa_interes, e.nombre AS nombre_estado
               FROM solicitud s
               JOIN tipo_prestamo t ON s.id_tipo_prestamo = t.id_tipo_prestamo
               JOIN estados e ON s.id_estado = e.id_estado
               WHERE UPPER(e.nombre) LIKE UPPER(:nombreEstado)
               ORDER BY %s %s
               LIMIT :size OFFSET :offset
               """.formatted(orderByColumn, direction);
    }

    private String getOrderByColumn(String sortBy) {
        return switch (sortBy) {
            case "monto" -> "s.monto";
            case "plazo" -> "s.plazo";
            case "email" -> "s.email";
            case "nombreTipoPrestamo" -> "t.nombre";
            case "tasaInteres" -> "t.tasa_interes";
            case "nombreEstado" -> "e.nombre";
            default -> "s.monto";
        };
    }
}
