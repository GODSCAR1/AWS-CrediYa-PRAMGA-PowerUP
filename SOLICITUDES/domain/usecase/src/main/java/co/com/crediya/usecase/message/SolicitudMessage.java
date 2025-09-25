package co.com.crediya.usecase.message;

public enum SolicitudMessage {

    ESTADO_PENDIENTE_REVISION_NO_ENCONTRADO("El estado 'Pendiente de Revision' no fue encontrado"),
    SOLICITUD_CREADA("Solicitud %s creada exitosamente"),
    SOLICITUD_NO_ENCONTRADA("No se encontró la solicitud con ID: %s"),
    SOLICITUD_PROCESADA("La solicitud ya ha sido procesada y no puede ser modificada.");
    private final String mensaje;

    SolicitudMessage(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
