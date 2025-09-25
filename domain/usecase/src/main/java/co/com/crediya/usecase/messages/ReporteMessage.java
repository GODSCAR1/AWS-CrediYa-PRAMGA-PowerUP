package co.com.crediya.usecase.messages;

public enum ReporteMessage {

    OBTENIENDO_REPORTE("Obteniendo reporte"),
    INCREMENTANDO_CONTADORES("Incrementando contadores"),

    ERRROR_ENVIANDO_REPORTE_DIARIO("Error enviando reporte diario: "),

    REPORTE_DIARIO_ENVIADO_EXITOSAMENTE("Reporte diario enviado exitosamente");
    private final String mensaje;

    ReporteMessage(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}

