package co.com.crediya.usecase.exception;

public class SecurityValidationException extends RuntimeException{
    public SecurityValidationException(String message) {
        super(message);
    }
}
