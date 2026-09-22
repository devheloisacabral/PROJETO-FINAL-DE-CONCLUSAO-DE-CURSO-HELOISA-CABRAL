package devheloisacabral.tcc.com.esportiva.application.enrollment;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.enrollment.Enrollment;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.enrollment.EnrollmentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.turma.TurmaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
public class EnrollmentService {
    private final EnrollmentRepository enrollments;
    private final AuthUserRepository users;
    private final MonthlyFeeRepository fees;
    private final TurmaRepository turmas;

    public EnrollmentService(EnrollmentRepository enrollments, AuthUserRepository users,
                             MonthlyFeeRepository fees, TurmaRepository turmas) {
        this.enrollments = enrollments;
        this.users = users;
        this.fees = fees;
        this.turmas = turmas;
    }

    @Transactional
    public EnrollmentResult create(Long studentId, Long turmaId, UserRole role) {
        requireAdmin(role);
        AuthUser student = users.findByIdAndRole(studentId, UserRole.ALUNO).orElseThrow(NotFoundException::new);
        Turma turma = turmas.findByIdForEnrollment(turmaId).orElseThrow(NotFoundException::new);
        if (!"ativo".equals(student.getStatus()) || !"ativa".equals(turma.getStatus())) {
            throw new InvalidEnrollmentException();
        }
        if (fees.existsByStudentIdAndStatusAndDueDateBefore(studentId, "pendente", LocalDate.now())) {
            throw new OverdueMonthlyFeeException();
        }
        if (enrollments.countByTurmaId(turmaId) >= turma.getMaxCapacity()) {
            throw new InvalidEnrollmentException();
        }
        return result(enrollments.save(new Enrollment(student, turma, LocalDateTime.now())));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResult> findAll(UserRole role, Long authenticatedUserId) {
        if (role == UserRole.ADMIN) return enrollments.findAllWithStudentAndTurma().stream().map(this::result).toList();
        if (role == UserRole.ALUNO) return enrollments.findAllWithStudentAndTurmaByStudentId(authenticatedUserId).stream().map(this::result).toList();
        throw new AccessDeniedException("enrollment access is not authorized");
    }

    private EnrollmentResult result(Enrollment enrollment) {
        AuthUser student = enrollment.getStudent();
        Turma turma = enrollment.getTurma();
        return new EnrollmentResult(enrollment.getId(),
                new StudentResult(student.getId(), student.getName(), student.getEmail(), student.getStatus()),
                new TurmaResult(turma.getId(), turma.getName(), turma.getStatus()), enrollment.getEnrolledAt());
    }

    private void requireAdmin(UserRole role) {
        if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized");
    }

    public record EnrollmentResult(Long id, StudentResult aluno, TurmaResult turma, LocalDateTime enrolledAt) {}
    public record StudentResult(Long id, String name, String email, String status) {}
    public record TurmaResult(Long id, String name, String status) {}
    public static class InvalidEnrollmentException extends RuntimeException {}
    public static class OverdueMonthlyFeeException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
}
