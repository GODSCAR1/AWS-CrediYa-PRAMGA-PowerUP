package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import co.com.crediya.usecase.usuario.message.ValidationMessage;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public class SalarioBaseValidator implements Validator<Usuario> {
    private static final BigDecimal SALARIO_BASE_MINIMO = BigDecimal.ZERO;
    private static final BigDecimal SALARIO_BASE_MAXIMO = BigDecimal.valueOf(15000000);
    @Override
    public Mono<Void> validate(Usuario usuario) {
        if(usuario.getSalarioBase() == null){
            return Mono.error(new UsuarioValidationException(ValidationMessage.SALARIO_BASE_OBLIGATORIO.getMensaje()));
        }
        if(usuario.getSalarioBase().compareTo(SALARIO_BASE_MINIMO) < 0){
            return Mono.error(new UsuarioValidationException(ValidationMessage.SALARIO_BASE_MAYOR_QUE_CERO.getMensaje()));
        }
        if(usuario.getSalarioBase().compareTo(SALARIO_BASE_MAXIMO) > 0){
            return Mono.error(new UsuarioValidationException(ValidationMessage.SALARIO_BASE_MENOR_QUE_15000000.getMensaje()));
        }
        return Mono.empty();
    }
}
