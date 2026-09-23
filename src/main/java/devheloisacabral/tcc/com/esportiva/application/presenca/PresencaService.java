package devheloisacabral.tcc.com.esportiva.application.presenca;

import devheloisacabral.tcc.com.esportiva.domain.aula.Aula;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.presenca.Presenca;
import devheloisacabral.tcc.com.esportiva.persistence.aula.AulaRepository;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.presenca.PresencaRepository;
import devheloisacabral.tcc.com.esportiva.persistence.enrollment.EnrollmentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class PresencaService {
    private final PresencaRepository presencas;
    private final AuthUserRepository users;
    private final MonthlyFeeRepository fees;
    private final AulaRepository aulas;
    private final EnrollmentRepository enrollments;

    public PresencaService(PresencaRepository presencas, AuthUserRepository users,
                           MonthlyFeeRepository fees, AulaRepository aulas) {
        this(presencas, users, fees, aulas, null);
    }

    public PresencaService(PresencaRepository presencas, AuthUserRepository users,
                           MonthlyFeeRepository fees, AulaRepository aulas, EnrollmentRepository enrollments) {
        this.presencas = presencas;
        this.users = users;
        this.fees = fees;
        this.aulas = aulas;
        this.enrollments = enrollments;
    }

    @Transactional
    public PresencaResult create(Long studentId, Long aulaId, UserRole role, Long authenticatedUserId) {
        if (role != UserRole.ADMIN && role != UserRole.PROFESSOR) {
            throw new AccessDeniedException("attendance creation is not authorized");
        }
        AuthUser student = users.findByIdAndRole(studentId, UserRole.ALUNO).orElseThrow(NotFoundException::new);
        Aula aula = aulas.findByIdWithTurma(aulaId).orElseThrow(NotFoundException::new);
        if (!"ativo".equals(student.getStatus())) throw new InactiveStudentException();
        if (enrollments != null && !enrollments.existsByStudentIdAndTurmaIdAndStatus(studentId, aula.getTurma().getId(), "ativa")) {
            throw new NotEnrolledException();
        }
        if (fees.existsByStudentIdAndStatusAndDueDateBefore(studentId, "pendente", LocalDate.now())) {
            throw new OverdueMonthlyFeeException();
        }
        if (role == UserRole.PROFESSOR && aula.getTurma().getProfessors().stream()
                .noneMatch(professor -> professor.getUser().getId().equals(authenticatedUserId))) {
            throw new AccessDeniedException("attendance lesson access is not authorized");
        }
        return result(presencas.save(new Presenca(student, aula, LocalDateTime.now())));
    }

    @Transactional(readOnly = true)
    public List<PresencaResult> findAll(UserRole role, Long authenticatedUserId, Long requestedStudentId) {
        return switch (role) {
            case ADMIN -> presencas.findAllWithDetails().stream().map(this::result).toList();
            case PROFESSOR -> presencas.findAllWithDetailsByProfessorUserId(authenticatedUserId).stream().map(this::result).toList();
            case ALUNO -> {
                if (requestedStudentId != null && !requestedStudentId.equals(authenticatedUserId)) {
                    throw new AccessDeniedException("student attendance access is not authorized");
                }
                yield presencas.findAllWithDetailsByStudentId(authenticatedUserId).stream().map(this::result).toList();
            }
        };
    }

    private PresencaResult result(Presenca presenca) {
        AuthUser student = presenca.getStudent();
        Aula aula = presenca.getAula();
        return new PresencaResult(presenca.getId(), new StudentResult(student.getId(), student.getName()),
                new AulaResult(aula.getId(), aula.getTurma().getId()), presenca.getRecordedAt());
    }

    public record PresencaResult(Long id, StudentResult aluno, AulaResult aula, LocalDateTime registradaEm) {}
    public record StudentResult(Long id, String name) {}
    public record AulaResult(Long id, Long turmaId) {}
    public static class InactiveStudentException extends RuntimeException {}
    public static class NotEnrolledException extends RuntimeException {}
    public static class OverdueMonthlyFeeException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
}
