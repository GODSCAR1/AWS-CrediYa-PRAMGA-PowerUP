package co.com.crediya.api.config;

import co.com.crediya.usecase.login.exception.LoginValidationException;
import co.com.crediya.usecase.usuario.exception.UsuarioNotFoundException;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

@Component
public class GlobalErrorAtributes extends DefaultErrorAttributes {
    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        Map<String, Object> atributes = super.getErrorAttributes(request, options);
        Throwable error = getError(request);

        if (error instanceof UsuarioValidationException || error instanceof LoginValidationException){
            atributes.put("status", HttpStatus.BAD_REQUEST.value());
            atributes.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
            atributes.put("message", error.getMessage());
        }
        else if (error instanceof UsuarioNotFoundException){
            atributes.put("status", HttpStatus.NOT_FOUND.value());
            atributes.put("error", HttpStatus.NOT_FOUND.getReasonPhrase());
            atributes.put("message", error.getMessage());
        }
        else{
            atributes.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            atributes.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
            atributes.put("message", "Ocurrio un error inesperado");
        }


        return atributes;
    }

}
