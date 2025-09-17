package co.com.crediya.usecase.message;

public enum ValidationMessage {

    ERROR_GENERICO("Error de validacion: %s"),

    MONTO_OBLIGATORIO("El monto es obligatorio"),
    MONTO_DEBE_SER_MAYOR_A_CERO("El monto debe ser mayor a cero"),

    PLAZO_DEBE_SER_MAYOR_A_CERO("El plazo debe ser mayor a 0"),

    NOMBRE_TIPO_PRESTAMO_OBLIGATORIO("El nombre del tipo de préstamo no puede ser nulo o vacio"),

    TIPO_PRESTAMO_NO_ENCONTRADO("El tipo de préstamo no fue encontrado"),

    MONTO_DEBE_ESTAR_ENTRE("El monto debe estar entre %s y %s");

    private final String mensaje;

    ValidationMessage(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
