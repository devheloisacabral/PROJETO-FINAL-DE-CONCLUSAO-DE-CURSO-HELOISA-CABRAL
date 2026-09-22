package devheloisacabral.tcc.com.esportiva.application.monthlyfee;

import devheloisacabral.tcc.com.esportiva.application.plan.PlanService.NotFoundException;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFee;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFeePayment;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeePaymentRepository;
import java.math.BigDecimal;
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
    private final MonthlyFeePaymentRepository payments;
    public MonthlyFeeService(MonthlyFeeRepository fees, AuthUserRepository users, PlanAssignmentRepository assignments, MonthlyFeePaymentRepository payments) { this.fees = fees; this.users = users; this.assignments = assignments; this.payments = payments; }
    public MonthlyFeeService(MonthlyFeeRepository fees, AuthUserRepository users) { this(fees, users, null); }
    public MonthlyFeeService(MonthlyFeeRepository fees, AuthUserRepository users, PlanAssignmentRepository assignments) { this(fees, users, assignments, null); }

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
    public FeeResult receive(Long id, BigDecimal amount, UserRole role) {
        requireAdmin(role); MonthlyFee fee = fees.findById(id).orElseThrow(NotFoundException::new);
        if (!"pendente".equals(fee.getStatus()) || amount == null || amount.signum() <= 0) throw new InvalidStatusException();
        BigDecimal totalPaid = totalPaid(id);
        BigDecimal remaining = fee.getAmount().subtract(totalPaid);
        if (amount.compareTo(remaining) > 0) throw new PaymentExceedsBalanceException();
        if (payments == null) {
            fee.pay();
            fee.getStudent().activate();
            return FeeResult.from(fee, amount);
        }
        payments.saveAndFlush(new MonthlyFeePayment(fee, amount));
        totalPaid = totalPaid.add(amount);
        fee.updateStatus(totalPaid);
        if ("paga".equals(fee.getStatus())) fee.getStudent().activate();
        return FeeResult.from(fee, totalPaid);
    }

    public FeeResult receive(Long id, UserRole role) {
        MonthlyFee fee = fees.findById(id).orElseThrow(NotFoundException::new);
        return receive(id, fee.getAmount(), role);
    }

    @Transactional
    public FeeResult cancel(Long id, UserRole role) {
        requireAdmin(role); MonthlyFee fee = fees.findById(id).orElseThrow(NotFoundException::new);
        if (!"paga".equals(fee.getStatus())) throw new InvalidStatusException();
        fee.cancelPayment(); return FeeResult.from(fee, totalPaid(id));
    }

    @Transactional
    public FeeResult cancelPayment(Long paymentId, UserRole role) {
        requireAdmin(role);
        MonthlyFeePayment payment = payments.findById(paymentId).orElseThrow(NotFoundException::new);
        if (!"valido".equals(payment.getStatus())) throw new InvalidStatusException();
        MonthlyFee fee = payment.getMonthlyFee();
        payment.cancel();
        BigDecimal totalPaid = totalPaid(fee.getId());
        fee.updateStatus(totalPaid);
        return FeeResult.from(fee, totalPaid);
    }

    @Transactional(readOnly = true)
    public List<FeeResult> find(Long userId, UserRole role) {
        if (role == UserRole.ADMIN) return fees.findAll().stream().map(fee -> FeeResult.from(fee, totalPaid(fee.getId()))).toList();
        if (role == UserRole.ALUNO) return fees.findAllByStudentId(userId).stream().map(fee -> FeeResult.from(fee, totalPaid(fee.getId()))).toList();
        throw new AccessDeniedException("financial data access is not authorized");
    }

    @Transactional(readOnly = true)
    public FeeResult findOne(Long id, Long userId, UserRole role) {
        if (role == UserRole.ADMIN) { MonthlyFee fee = fees.findById(id).orElseThrow(NotFoundException::new); return FeeResult.from(fee, totalPaid(id)); }
        if (role == UserRole.ALUNO) { MonthlyFee fee = fees.findByIdAndStudentId(id, userId).orElseThrow(NotFoundException::new); return FeeResult.from(fee, totalPaid(id)); }
        throw new AccessDeniedException("financial data access is not authorized");
    }

    private void requireAdmin(UserRole role) { if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized"); }
    private BigDecimal totalPaid(Long feeId) {
        if (payments == null) return BigDecimal.ZERO;
        return payments.findAllByMonthlyFeeId(feeId).stream()
                .filter(payment -> "valido".equals(payment.getStatus()))
                .map(MonthlyFeePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public record FeeResult(Long id, Long studentId, LocalDate referenceMonth, LocalDate dueDate, BigDecimal amount, String status, BigDecimal totalPaid) {
        static FeeResult from(MonthlyFee f) { return from(f, BigDecimal.ZERO); }
        static FeeResult from(MonthlyFee f, BigDecimal totalPaid) { return new FeeResult(f.getId(), f.getStudent().getId(), f.getReferenceMonth(), f.getDueDate(), f.getAmount(), f.getStatus(), totalPaid); }
    }
    public static class MissingPlanException extends RuntimeException {}
    public static class DuplicateFeeException extends RuntimeException {}
    public static class InvalidStatusException extends RuntimeException {}
    public static class PaymentExceedsBalanceException extends RuntimeException {}
}
