package devheloisacabral.tcc.com.esportiva.application.plan;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.plan.Plan;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.plan.PlanRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class PlanService {
    private final PlanRepository plans;
    private final AuthUserRepository users;
    public PlanService(PlanRepository plans, AuthUserRepository users) { this.plans = plans; this.users = users; }

    @Transactional
    public PlanResult create(String name, BigDecimal amount, UserRole role) {
        requireAdmin(role);
        return PlanResult.from(plans.save(new Plan(name.trim(), amount)));
    }

    @Transactional
    public void associate(Long studentId, Long planId, UserRole role) {
        requireAdmin(role);
        AuthUser student = users.findByIdAndRole(studentId, UserRole.ALUNO).orElseThrow(NotFoundException::new);
        student.setPlan(plans.findById(planId).orElseThrow(NotFoundException::new));
        users.save(student);
    }

    private void requireAdmin(UserRole role) { if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized"); }
    public record PlanResult(Long id, String name, BigDecimal amount) { static PlanResult from(Plan p) { return new PlanResult(p.getId(), p.getName(), p.getAmount()); } }
    public static class NotFoundException extends RuntimeException {}
}
