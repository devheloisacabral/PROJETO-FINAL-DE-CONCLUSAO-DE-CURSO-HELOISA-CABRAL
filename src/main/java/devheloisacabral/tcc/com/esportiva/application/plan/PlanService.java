package devheloisacabral.tcc.com.esportiva.application.plan;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.plan.Plan;
import devheloisacabral.tcc.com.esportiva.domain.plan.PlanAssignment;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.plan.PlanRepository;
import devheloisacabral.tcc.com.esportiva.persistence.plan.PlanAssignmentRepository;
import java.time.LocalDate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class PlanService {
    private final PlanRepository plans;
    private final AuthUserRepository users;
    private final PlanAssignmentRepository assignments;
    public PlanService(PlanRepository plans, AuthUserRepository users, PlanAssignmentRepository assignments) { this.plans = plans; this.users = users; this.assignments = assignments; }

    @Transactional
    public PlanResult create(String name, BigDecimal amount, UserRole role) {
        requireAdmin(role);
        return PlanResult.from(plans.save(new Plan(name.trim(), amount)));
    }

    @Transactional
    public void associate(Long studentId, Long planId, LocalDate validFrom, UserRole role) {
        requireAdmin(role);
        AuthUser student = users.findByIdAndRole(studentId, UserRole.ALUNO).orElseThrow(NotFoundException::new);
        Plan plan = plans.findById(planId).orElseThrow(NotFoundException::new);
        if (assignments.existsByStudentIdAndValidFrom(studentId, validFrom)) throw new DuplicatePlanAssignmentException();
        try {
            assignments.saveAndFlush(new PlanAssignment(student, plan, validFrom));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicatePlanAssignmentException();
        }
        if (!validFrom.isAfter(LocalDate.now())) {
            student.setPlan(plan);
            users.save(student);
        }
    }

    @Transactional
    public void associate(Long studentId, Long planId, UserRole role) {
        associate(studentId, planId, LocalDate.now(), role);
    }

    private void requireAdmin(UserRole role) { if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized"); }
    public record PlanResult(Long id, String name, BigDecimal amount) { static PlanResult from(Plan p) { return new PlanResult(p.getId(), p.getName(), p.getAmount()); } }
    public static class NotFoundException extends RuntimeException {}
    public static class DuplicatePlanAssignmentException extends RuntimeException {}
}
