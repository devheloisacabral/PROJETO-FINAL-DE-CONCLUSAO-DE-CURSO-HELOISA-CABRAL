package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.auth.AuthenticationService.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class AuthenticationExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_credentials"));
    }
}
