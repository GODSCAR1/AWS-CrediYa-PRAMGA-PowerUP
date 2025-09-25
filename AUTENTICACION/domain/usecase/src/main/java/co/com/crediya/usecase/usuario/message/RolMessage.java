package co.com.crediya.usecase.usuario.message;

public enum RolMessage {
    ROL_NO_ENCONTRADO("El rol no fue encontrado");
    private final String mensaje;

    RolMessage(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
