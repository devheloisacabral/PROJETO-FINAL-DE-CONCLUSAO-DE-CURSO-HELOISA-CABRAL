package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.auth.AuthenticationService.InvalidCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;
import devheloisacabral.tcc.com.esportiva.application.plan.PlanService.NotFoundException;
import devheloisacabral.tcc.com.esportiva.application.plan.PlanService.DuplicatePlanAssignmentException;
import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService.DuplicateFeeException;
import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService.MissingPlanException;
import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService.InvalidStatusException;
import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService.PaymentExceedsBalanceException;
import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService.RefundAlreadyExistsException;
import devheloisacabral.tcc.com.esportiva.application.turma.TurmaService.InvalidTurmaException;
import devheloisacabral.tcc.com.esportiva.application.enrollment.EnrollmentService.InvalidEnrollmentException;
import devheloisacabral.tcc.com.esportiva.application.enrollment.EnrollmentService;
import devheloisacabral.tcc.com.esportiva.application.aula.AulaService;
import devheloisacabral.tcc.com.esportiva.application.presenca.PresencaService;
import devheloisacabral.tcc.com.esportiva.application.dashboard.DashboardService;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser.InvalidDueDayException;

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
    @ExceptionHandler(DuplicatePlanAssignmentException.class)
    public ResponseEntity<Map<String, String>> duplicatePlanAssignment() { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "plan_change_already_exists")); }
    @ExceptionHandler({MissingPlanException.class, InvalidStatusException.class, PaymentExceedsBalanceException.class, RefundAlreadyExistsException.class})
    public ResponseEntity<Map<String, String>> invalidOperation(RuntimeException ex) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "invalid_monthly_fee_operation")); }
    @ExceptionHandler({NotFoundException.class, devheloisacabral.tcc.com.esportiva.application.professor.ProfessorService.NotFoundException.class,
            devheloisacabral.tcc.com.esportiva.application.turma.TurmaService.NotFoundException.class})
    public ResponseEntity<Map<String, String>> notFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "resource_not_found")); }

    @ExceptionHandler(InvalidTurmaException.class)
    public ResponseEntity<Map<String, String>> invalidTurma() { return ResponseEntity.badRequest().body(Map.of("error", "invalid_turma")); }

    @ExceptionHandler(InvalidEnrollmentException.class)
    public ResponseEntity<Map<String, String>> invalidEnrollment() { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "invalid_enrollment")); }

    @ExceptionHandler({EnrollmentService.OverdueMonthlyFeeException.class, PresencaService.OverdueMonthlyFeeException.class})
    public ResponseEntity<Map<String, String>> overdueMonthlyFee() { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "student_in_debt")); }

    @ExceptionHandler(EnrollmentService.NotFoundException.class)
    public ResponseEntity<Map<String, String>> enrollmentNotFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "resource_not_found")); }

    @ExceptionHandler(AulaService.NotFoundException.class)
    public ResponseEntity<Map<String, String>> aulaNotFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "resource_not_found")); }

    @ExceptionHandler(AulaService.WeekdayMismatchException.class)
    public ResponseEntity<Map<String, String>> weekdayMismatch() { return ResponseEntity.badRequest().body(Map.of("error", "date_does_not_match_turma_weekday")); }

    @ExceptionHandler(AulaService.InvalidAulaException.class)
    public ResponseEntity<Map<String, String>> invalidAula() { return ResponseEntity.badRequest().body(Map.of("error", "invalid_aula")); }

    @ExceptionHandler(PresencaService.InactiveStudentException.class)
    public ResponseEntity<Map<String, String>> inactiveStudent() { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "inactive_student")); }

    @ExceptionHandler(PresencaService.NotEnrolledException.class)
    public ResponseEntity<Map<String, String>> notEnrolled() { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "student_not_enrolled")); }

    @ExceptionHandler(PresencaService.NotFoundException.class)
    public ResponseEntity<Map<String, String>> presencaNotFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "resource_not_found")); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> accessDenied() { return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "access_denied")); }

    @ExceptionHandler(DashboardService.InvalidMonthException.class)
    public ResponseEntity<Map<String, String>> invalidMonth() { return ResponseEntity.badRequest().body(Map.of("error", "invalid_reference_month")); }

    @ExceptionHandler(InvalidDueDayException.class)
    public ResponseEntity<Map<String, String>> invalidDueDay() { return ResponseEntity.badRequest().body(Map.of("error", "invalid_due_day")); }

    @ExceptionHandler(devheloisacabral.tcc.com.esportiva.application.student.StudentService.StudentNotFoundException.class)
    public ResponseEntity<Map<String, String>> studentNotFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "resource_not_found")); }
}
