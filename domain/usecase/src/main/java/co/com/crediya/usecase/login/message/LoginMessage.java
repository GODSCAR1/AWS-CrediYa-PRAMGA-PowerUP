package co.com.crediya.usecase.login.message;

public enum LoginMessage {

    USUARIO_NO_ENCONTRADO("Usuario no encontrado"),
    CREDENCIALES_INVALIDAS("Credenciales invalidas"),
    USUARIO_LOGEADO("Usuario %s logueado exitosamente");
    private final String mensaje;

    LoginMessage(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
