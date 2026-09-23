package devheloisacabral.tcc.com.esportiva.application.monthlyfee;

import devheloisacabral.tcc.com.esportiva.application.plan.PlanService.NotFoundException;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFee;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFeePayment;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFeeRefund;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeePaymentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRefundRepository;
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
    private final MonthlyFeeRefundRepository refunds;
    public MonthlyFeeService(MonthlyFeeRepository fees, AuthUserRepository users, PlanAssignmentRepository assignments, MonthlyFeePaymentRepository payments, MonthlyFeeRefundRepository refunds) { this.fees = fees; this.users = users; this.assignments = assignments; this.payments = payments; this.refunds = refunds; }
    public MonthlyFeeService(MonthlyFeeRepository fees, AuthUserRepository users) { this(fees, users, null); }
    public MonthlyFeeService(MonthlyFeeRepository fees, AuthUserRepository users, PlanAssignmentRepository assignments) { this(fees, users, assignments, null, null); }
    public MonthlyFeeService(MonthlyFeeRepository fees, AuthUserRepository users, PlanAssignmentRepository assignments, MonthlyFeePaymentRepository payments) { this(fees, users, assignments, payments, null); }

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
        FinancialSummary summary = financialSummary(id);
        BigDecimal remaining = fee.getAmount().subtract(summary.netPaid());
        if (amount.compareTo(remaining) > 0) throw new PaymentExceedsBalanceException();
        if (payments == null) {
            fee.pay();
            fee.getStudent().activate();
            return FeeResult.from(fee, financialSummary(id, amount, BigDecimal.ZERO));
        }
        payments.saveAndFlush(new MonthlyFeePayment(fee, amount));
        summary = financialSummary(id, summary.totalPaid().add(amount), summary.totalRefunded());
        fee.updateStatus(summary.netPaid());
        if ("paga".equals(fee.getStatus())) fee.getStudent().activate();
        return FeeResult.from(fee, summary);
    }

    public FeeResult receive(Long id, UserRole role) {
        MonthlyFee fee = fees.findById(id).orElseThrow(NotFoundException::new);
        return receive(id, fee.getAmount(), role);
    }

    @Transactional
    public FeeResult cancel(Long id, UserRole role) {
        requireAdmin(role); MonthlyFee fee = fees.findById(id).orElseThrow(NotFoundException::new);
        if (!"paga".equals(fee.getStatus())) throw new InvalidStatusException();
        if (payments != null && refunds != null) {
            List<MonthlyFeePayment> validPayments = payments.findAllByMonthlyFeeId(id).stream()
                    .filter(payment -> "valido".equals(payment.getStatus()))
                    .filter(payment -> refunds.findAllByPaymentId(payment.getId()).isEmpty())
                    .toList();
            if (validPayments.isEmpty()) throw new RefundAlreadyExistsException();
            validPayments.forEach(payment -> refunds.saveAndFlush(new MonthlyFeeRefund(payment)));
            FinancialSummary summary = financialSummary(id);
            fee.updateStatus(summary.netPaid());
            return FeeResult.from(fee, summary);
        }
        FinancialSummary summary = financialSummary(id);
        fee.cancelPayment(); return FeeResult.from(fee, summary);
    }

    @Transactional
    public FeeResult cancelPayment(Long paymentId, UserRole role) {
        return refundPayment(paymentId, role);
    }

    @Transactional
    public FeeResult refundPayment(Long paymentId, UserRole role) {
        requireAdmin(role);
        MonthlyFeePayment payment = payments.findById(paymentId).orElseThrow(NotFoundException::new);
        if (!"valido".equals(payment.getStatus())) throw new InvalidStatusException();
        if (!refunds.findAllByPaymentId(paymentId).isEmpty()) throw new RefundAlreadyExistsException();
        MonthlyFee fee = payment.getMonthlyFee();
        refunds.saveAndFlush(new MonthlyFeeRefund(payment));
        FinancialSummary summary = financialSummary(fee.getId());
        fee.updateStatus(summary.netPaid());
        return FeeResult.from(fee, summary);
    }

    @Transactional(readOnly = true)
    public List<FeeResult> find(Long userId, UserRole role) {
        if (role == UserRole.ADMIN) return fees.findAll().stream().map(fee -> FeeResult.from(fee, financialSummary(fee.getId()))).toList();
        if (role == UserRole.ALUNO) return fees.findAllByStudentId(userId).stream().map(fee -> FeeResult.from(fee, financialSummary(fee.getId()))).toList();
        throw new AccessDeniedException("financial data access is not authorized");
    }

    @Transactional(readOnly = true)
    public FeeResult findOne(Long id, Long userId, UserRole role) {
        if (role == UserRole.ADMIN) { MonthlyFee fee = fees.findById(id).orElseThrow(NotFoundException::new); return FeeResult.from(fee, financialSummary(id)); }
        if (role == UserRole.ALUNO) { MonthlyFee fee = fees.findByIdAndStudentId(id, userId).orElseThrow(NotFoundException::new); return FeeResult.from(fee, financialSummary(id)); }
        throw new AccessDeniedException("financial data access is not authorized");
    }

    private void requireAdmin(UserRole role) { if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized"); }
    private FinancialSummary financialSummary(Long feeId) {
        if (payments == null || refunds == null) return new FinancialSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        BigDecimal totalPaid = payments.findAllByMonthlyFeeId(feeId).stream()
                .filter(payment -> "valido".equals(payment.getStatus()))
                .map(MonthlyFeePayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRefunded = refunds.findAllByPaymentMonthlyFeeId(feeId).stream()
                .map(MonthlyFeeRefund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return financialSummary(feeId, totalPaid, totalRefunded);
    }
    private FinancialSummary financialSummary(Long feeId, BigDecimal totalPaid, BigDecimal totalRefunded) {
        return new FinancialSummary(totalPaid, totalRefunded, totalPaid.subtract(totalRefunded));
    }
    public record FinancialSummary(BigDecimal totalPaid, BigDecimal totalRefunded, BigDecimal netPaid) {}
    public record FeeResult(Long id, Long studentId, LocalDate referenceMonth, LocalDate dueDate, BigDecimal amount, String status, BigDecimal totalPaid, BigDecimal totalRefunded, BigDecimal netPaid) {
        static FeeResult from(MonthlyFee f) { return from(f, new FinancialSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)); }
        static FeeResult from(MonthlyFee f, FinancialSummary summary) { return new FeeResult(f.getId(), f.getStudent().getId(), f.getReferenceMonth(), f.getDueDate(), f.getAmount(), f.getStatus(), summary.totalPaid(), summary.totalRefunded(), summary.netPaid()); }
    }
    public static class MissingPlanException extends RuntimeException {}
    public static class DuplicateFeeException extends RuntimeException {}
    public static class InvalidStatusException extends RuntimeException {}
    public static class PaymentExceedsBalanceException extends RuntimeException {}
    public static class RefundAlreadyExistsException extends RuntimeException {}
}
