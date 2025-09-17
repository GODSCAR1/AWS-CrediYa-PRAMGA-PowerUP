package co.com.crediya.sqs.listener.message;

public enum SQSMessage {

    MENSAJE_RECIBIDO("Mensaje recibido: %s"),
    MENSAJE_PROCESADO("El mensaje ha sido procesado exitosamente"),
    ERROR_PROCESANDO_MENSAJE("Ocurrió un error al procesar el mensaje.");

    private final String mensaje;

    SQSMessage(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
