package co.com.crediya.usecase.usuario.message;

public enum UsuarioMessage {

    USUARIO_CREADO("Usuario %s creado exitosamente"),

    USUARIOS_NO_ENCONTRADOS("No se encontraron usuarios"),

    USUARIOS_OBTENIDOS("Usuarios obtenidos exitosamente");
    private final String mensaje;

    UsuarioMessage(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
