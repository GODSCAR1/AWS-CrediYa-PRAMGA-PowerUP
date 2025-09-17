package co.com.crediya.usecase.login.message;

public enum ValidationMessage {
    ERROR_GENERICO("Error de validacion: %s"),
    EMAIL_OBLIGATORIO("El email es obligatorio"),
    EMAIL_INVALIDO("El email no es válido"),
    CONTRASENA_OBLIGATORIA("La contraseña es obligatoria");
    private final String mensaje;

    ValidationMessage(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
