package co.com.crediya.usecase.usuario.validation;

import co.com.crediya.model.Usuario;
import co.com.crediya.usecase.Validator;
import co.com.crediya.usecase.usuario.exception.UsuarioValidationException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public class SalarioBaseValidator implements Validator<Usuario> {
    private static final BigDecimal SALARIO_BASE_MINIMO = BigDecimal.ZERO;
    private static final BigDecimal SALARIO_BASE_MAXIMO = BigDecimal.valueOf(15000000);
    @Override
    public Mono<Void> validate(Usuario usuario) {
        if(usuario.getSalarioBase() == null){
            return Mono.error(new UsuarioValidationException("El salario base es obligatorio"));
        }
        if(usuario.getSalarioBase().compareTo(SALARIO_BASE_MINIMO) < 0){
            return Mono.error(new UsuarioValidationException("El salario base debe ser mayor o igual a 0"));
        }
        if(usuario.getSalarioBase().compareTo(SALARIO_BASE_MAXIMO) > 0){
            return Mono.error(new UsuarioValidationException("El salario base debe ser menor o igual a 15000000"));
        }
        return Mono.empty();
    }
}
