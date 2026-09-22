package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.auth.AuthenticationService.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Map;
import devheloisacabral.tcc.com.esportiva.application.plan.PlanService.NotFoundException;
import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService.DuplicateFeeException;
import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService.MissingPlanException;
import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService.InvalidStatusException;
import devheloisacabral.tcc.com.esportiva.application.turma.TurmaService.InvalidTurmaException;

@RestControllerAdvice
public class AuthenticationExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> invalidCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid_credentials"));
    }

    @ExceptionHandler({devheloisacabral.tcc.com.esportiva.application.student.StudentService.EmailAlreadyUsedException.class,
            devheloisacabral.tcc.com.esportiva.application.professor.ProfessorService.EmailAlreadyUsedException.class,
            DataIntegrityViolationException.class})
    public ResponseEntity<Map<String, String>> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "email_already_used"));
    }

    @ExceptionHandler(DuplicateFeeException.class)
    public ResponseEntity<Map<String, String>> duplicateFee() { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "monthly_fee_already_exists")); }
    @ExceptionHandler({MissingPlanException.class, InvalidStatusException.class})
    public ResponseEntity<Map<String, String>> invalidOperation(RuntimeException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "invalid_monthly_fee_operation")); }
    @ExceptionHandler({NotFoundException.class, devheloisacabral.tcc.com.esportiva.application.professor.ProfessorService.NotFoundException.class,
            devheloisacabral.tcc.com.esportiva.application.turma.TurmaService.NotFoundException.class})
    public ResponseEntity<Map<String, String>> notFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "resource_not_found")); }

    @ExceptionHandler(InvalidTurmaException.class)
    public ResponseEntity<Map<String, String>> invalidTurma() { return ResponseEntity.badRequest().body(Map.of("error", "invalid_turma")); }
}
