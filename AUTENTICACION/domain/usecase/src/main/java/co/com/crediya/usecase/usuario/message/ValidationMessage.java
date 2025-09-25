package co.com.crediya.usecase.usuario.message;

public enum ValidationMessage {

    ERROR_GENERICO("Error de validacion: %s"),
    NOMBRE_OBLIGATORIO("El nombre es obligatorio"),
    APELLIDO_OBLIGATORIO("El apellido es obligatorio"),
    EMAIL_OBLIGATORIO("El email es obligatorio"),
    EMAIL_INVALIDO("El email es invalido"),
    EMAIL_EXISTENTE("El email ya existe"),
    CONTRASENA_OBLIGATORIA("La contraseña es obligatoria"),
    DOCUMENTO_IDENTIDAD_OBLIGATORIO("El documento de identidad es obligatorio"),
    DOCUMENTO_IDENTIDAD_EXISTENTE("El documento de identidad ya existe"),

    SALARIO_BASE_OBLIGATORIO("El salario base es obligatorio"),
    SALARIO_BASE_MAYOR_QUE_CERO("El salario base debe ser mayor que cero"),

    SALARIO_BASE_MENOR_QUE_15000000("El salario base debe ser menor que 15000000");


    private final String mensaje;

    ValidationMessage(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
