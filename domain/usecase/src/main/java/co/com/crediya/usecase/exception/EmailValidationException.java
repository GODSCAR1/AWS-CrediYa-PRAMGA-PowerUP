package co.com.crediya.usecase.exception;

public class EmailValidationException extends RuntimeException {
    public EmailValidationException(String message) {
        super(message);
    }
}
