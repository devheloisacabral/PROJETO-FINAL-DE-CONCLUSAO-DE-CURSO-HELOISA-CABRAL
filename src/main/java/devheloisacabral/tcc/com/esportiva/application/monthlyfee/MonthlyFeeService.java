package devheloisacabral.tcc.com.esportiva.application.monthlyfee;

import devheloisacabral.tcc.com.esportiva.application.plan.PlanService.NotFoundException;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFee;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.plan.PlanAssignmentRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyFeeService {
    private final MonthlyFeeRepository fees;
    private final AuthUserRepository users;
    private final PlanAssignmentRepository assignments;
    public MonthlyFeeService(MonthlyFeeRepository fees, AuthUserRepository users, PlanAssignmentRepository assignments) { this.fees = fees; this.users = users; this.assignments = assignments; }
    public MonthlyFeeService(MonthlyFeeRepository fees, AuthUserRepository users) { this(fees, users, null); }

    @Transactional
    public FeeResult create(Long studentId, LocalDate referenceMonth, UserRole role) {
        requireAdmin(role);
        AuthUser student = users.findByIdAndRole(studentId, UserRole.ALUNO).orElseThrow(NotFoundException::new);
        LocalDate month = referenceMonth.withDayOfMonth(1);
        var plan = assignments == null
                ? student.getPlan()
                : assignments.findTopByStudentIdAndValidFromLessThanEqualOrderByValidFromDesc(studentId, month).map(a -> a.getPlan()).orElse(null);
        if (plan == null) throw new MissingPlanException();
        try { return FeeResult.from(fees.saveAndFlush(new MonthlyFee(student, month, plan.getAmount(), student.getDueDay()))); }
        catch (DataIntegrityViolationException ex) { throw new DuplicateFeeException(); }
    }

    @Transactional
    public FeeResult receive(Long id, UserRole role) {
        requireAdmin(role); MonthlyFee fee = fees.findById(id).orElseThrow(NotFoundException::new);
        if (!"pendente".equals(fee.getStatus())) throw new InvalidStatusException();
        fee.pay(); fee.getStudent().activate(); return FeeResult.from(fee);
    }

    @Transactional
    public FeeResult cancel(Long id, UserRole role) {
        requireAdmin(role); MonthlyFee fee = fees.findById(id).orElseThrow(NotFoundException::new);
        if (!"paga".equals(fee.getStatus())) throw new InvalidStatusException();
        fee.cancelPayment(); return FeeResult.from(fee);
    }

    @Transactional(readOnly = true)
    public List<FeeResult> find(Long userId, UserRole role) {
        if (role == UserRole.ADMIN) return fees.findAll().stream().map(FeeResult::from).toList();
        if (role == UserRole.ALUNO) return fees.findAllByStudentId(userId).stream().map(FeeResult::from).toList();
        throw new AccessDeniedException("financial data access is not authorized");
    }

    @Transactional(readOnly = true)
    public FeeResult findOne(Long id, Long userId, UserRole role) {
        if (role == UserRole.ADMIN) return FeeResult.from(fees.findById(id).orElseThrow(NotFoundException::new));
        if (role == UserRole.ALUNO) return FeeResult.from(fees.findByIdAndStudentId(id, userId).orElseThrow(NotFoundException::new));
        throw new AccessDeniedException("financial data access is not authorized");
    }

    private void requireAdmin(UserRole role) { if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized"); }
    public record FeeResult(Long id, Long studentId, LocalDate referenceMonth, LocalDate dueDate, java.math.BigDecimal amount, String status) { static FeeResult from(MonthlyFee f) { return new FeeResult(f.getId(), f.getStudent().getId(), f.getReferenceMonth(), f.getDueDate(), f.getAmount(), f.getStatus()); } }
    public static class MissingPlanException extends RuntimeException {}
    public static class DuplicateFeeException extends RuntimeException {}
    public static class InvalidStatusException extends RuntimeException {}
}
