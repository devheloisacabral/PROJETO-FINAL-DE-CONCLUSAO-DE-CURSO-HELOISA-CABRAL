package devheloisacabral.tcc.com.esportiva.application.enrollment;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.enrollment.Enrollment;
import devheloisacabral.tcc.com.esportiva.domain.enrollment.WaitlistEntry;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.enrollment.EnrollmentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.enrollment.WaitlistRepository;
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
    private final WaitlistRepository waitlist;

    public EnrollmentService(EnrollmentRepository enrollments, AuthUserRepository users,
                             MonthlyFeeRepository fees, TurmaRepository turmas, WaitlistRepository waitlist) {
        this.enrollments = enrollments;
        this.users = users;
        this.fees = fees;
        this.turmas = turmas;
        this.waitlist = waitlist;
    }

    @Transactional
    public EnrollmentResult create(Long studentId, Long turmaId, UserRole role) {
        return (EnrollmentResult) createOrWaitlist(studentId, turmaId, false, role);
    }

    @Transactional
    public Object createOrWaitlist(Long studentId, Long turmaId, boolean acceptWaitlist, UserRole role) {
        requireAdmin(role);
        AuthUser student = users.findByIdAndRole(studentId, UserRole.ALUNO).orElseThrow(NotFoundException::new);
        Turma turma = turmas.findByIdForEnrollment(turmaId).orElseThrow(NotFoundException::new);
        if (!"ativo".equals(student.getStatus()) || !"ativa".equals(turma.getStatus())) {
            throw new InvalidEnrollmentException();
        }
        if (fees.existsByStudentIdAndStatusAndDueDateBefore(studentId, "pendente", LocalDate.now())) {
            throw new OverdueMonthlyFeeException();
        }
        if (enrollments.existsByStudentIdAndTurmaIdAndStatus(studentId, turmaId, "ativa")) {
            throw new InvalidEnrollmentException();
        }
        if (enrollments.countByTurmaIdAndStatus(turmaId, "ativa") >= turma.getMaxCapacity()) {
            if (acceptWaitlist) {
                if (waitlist.existsByStudentIdAndTurmaIdAndStatus(studentId, turmaId, "ativa")) {
                    throw new InvalidEnrollmentException();
                }
                WaitlistEntry entry = waitlist.save(new WaitlistEntry(student, turma, LocalDateTime.now()));
                return new WaitlistResult(entry.getId(), student.getId(), turma.getId(), entry.getRequestedAt(), entry.getStatus());
            }
            throw new InvalidEnrollmentException();
        }
        return result(enrollments.save(new Enrollment(student, turma, LocalDateTime.now())));
    }

    @Transactional
    public EnrollmentResult cancel(Long id, UserRole role) {
        requireAdmin(role);
        Enrollment enrollment = enrollments.findById(id).orElseThrow(NotFoundException::new);
        if (!"ativa".equals(enrollment.getStatus())) throw new InvalidEnrollmentException();
        Turma turma = turmas.findByIdForEnrollment(enrollment.getTurma().getId()).orElse(enrollment.getTurma());
        enrollment.cancel();
        promoteFirstEligible(turma);
        return result(enrollment);
    }

    private void promoteFirstEligible(Turma turma) {
        for (WaitlistEntry entry : waitlist.findByTurmaIdAndStatusOrderByRequestedAtAscIdAsc(turma.getId(), "ativa")) {
            Long studentId = entry.getStudent().getId();
            if (!"ativo".equals(entry.getStudent().getStatus())
                    || enrollments.existsByStudentIdAndTurmaIdAndStatus(studentId, turma.getId(), "ativa")
                    || fees.existsByStudentIdAndStatusAndDueDateBefore(studentId, "pendente", LocalDate.now())) {
                continue;
            }
            enrollments.save(new Enrollment(entry.getStudent(), turma, LocalDateTime.now()));
            entry.promote();
            waitlist.save(entry);
            return;
        }
    }

    @Transactional
    public TransferResult transfer(Long enrollmentId, Long destinationTurmaId, UserRole role) {
        requireAdmin(role);
        Enrollment origin = enrollments.findById(enrollmentId).orElseThrow(NotFoundException::new);
        if (!"ativa".equals(origin.getStatus())) throw new InvalidEnrollmentException();
        if (origin.getTurma().getId().equals(destinationTurmaId)) throw new InvalidEnrollmentException();
        Turma destination = turmas.findByIdForEnrollment(destinationTurmaId).orElseThrow(NotFoundException::new);
        if (!"ativa".equals(destination.getStatus())) throw new InvalidEnrollmentException();
        if (enrollments.existsByStudentIdAndTurmaIdAndStatus(origin.getStudent().getId(), destinationTurmaId, "ativa")) {
            throw new InvalidEnrollmentException();
        }
        if (enrollments.countByTurmaIdAndStatus(destinationTurmaId, "ativa") >= destination.getMaxCapacity()) {
            throw new InvalidEnrollmentException();
        }
        origin.cancel();
        Enrollment target = enrollments.save(new Enrollment(origin.getStudent(), destination, LocalDateTime.now()));
        return new TransferResult(result(origin), result(target));
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
                new TurmaResult(turma.getId(), turma.getName(), turma.getStatus()), enrollment.getEnrolledAt(), enrollment.getStatus());
    }

    private void requireAdmin(UserRole role) {
        if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized");
    }

    public record EnrollmentResult(Long id, StudentResult aluno, TurmaResult turma, LocalDateTime enrolledAt, String status) {}
    public record TransferResult(EnrollmentResult origem, EnrollmentResult destino) {}
    public record WaitlistResult(Long id, Long alunoId, Long turmaId, LocalDateTime requestedAt, String status) {}
    public record StudentResult(Long id, String name, String email, String status) {}
    public record TurmaResult(Long id, String name, String status) {}
    public static class InvalidEnrollmentException extends RuntimeException {}
    public static class OverdueMonthlyFeeException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
}
