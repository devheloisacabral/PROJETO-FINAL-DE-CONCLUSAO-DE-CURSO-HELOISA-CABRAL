package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService;
import devheloisacabral.tcc.com.esportiva.application.plan.PlanService;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFee;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFeePayment;
import devheloisacabral.tcc.com.esportiva.domain.plan.Plan;
import devheloisacabral.tcc.com.esportiva.domain.plan.PlanAssignment;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeePaymentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.plan.PlanAssignmentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.plan.PlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanAndMonthlyFeeServiceTest {
    @Mock PlanRepository plans;
    @Mock AuthUserRepository users;
    @Mock MonthlyFeeRepository fees;
    @Mock MonthlyFeePaymentRepository payments;
    @Mock PlanAssignmentRepository assignments;
    @InjectMocks PlanService planService;
    @InjectMocks MonthlyFeeService feeService;

    @Test void adminCreatesPlanAndOtherRolesAreDenied() {
        when(plans.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("Mensal", planService.create("Mensal", new BigDecimal("120.00"), UserRole.ADMIN).name());
        assertThrows(AccessDeniedException.class, () -> planService.create("Mensal", BigDecimal.ONE, UserRole.PROFESSOR));
        verify(plans, times(1)).save(any());
    }

    @Test void monthlyFeeUsesPlanValueAndIndividualDueDay() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        student.setPlan(new Plan("Mensal", new BigDecimal("120.00")));
        student.setDueDay(20);
        Plan plan = new Plan("Mensal", new BigDecimal("120.00"));
        when(assignments.findTopByStudentIdAndValidFromLessThanEqualOrderByValidFromDesc(7L, LocalDate.of(2026, 9, 1)))
                .thenReturn(Optional.of(new PlanAssignment(student, plan, LocalDate.of(2026, 1, 1))));
        when(users.findByIdAndRole(7L, UserRole.ALUNO)).thenReturn(Optional.of(student));
        when(fees.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var result = feeService.create(7L, LocalDate.of(2026, 9, 1), UserRole.ADMIN);
        assertEquals(new BigDecimal("120.00"), result.amount());
        assertEquals(LocalDate.of(2026, 9, 20), result.dueDate());
        assertEquals("pendente", result.status());
    }

    @Test void futurePlanChangeKeepsPreviousPlanUntilItsValidityDate() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        Plan basic = new Plan("Básico", new BigDecimal("100.00"));
        Plan premium = new Plan("Premium", new BigDecimal("180.00"));
        student.setPlan(basic);
        when(users.findByIdAndRole(7L, UserRole.ALUNO)).thenReturn(Optional.of(student));
        when(plans.findById(2L)).thenReturn(Optional.of(premium));
        when(assignments.existsByStudentIdAndValidFrom(7L, LocalDate.of(2026, 11, 1))).thenReturn(false);
        when(assignments.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        planService.associate(7L, 2L, LocalDate.of(2026, 11, 1), UserRole.ADMIN);

        assertSame(basic, student.getPlan());
        verify(assignments).saveAndFlush(any(PlanAssignment.class));
        verify(users, never()).save(student);
    }

    @Test void monthlyFeeUsesPlanValidInReferenceMonth() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        Plan premium = new Plan("Premium", new BigDecimal("180.00"));
        when(users.findByIdAndRole(7L, UserRole.ALUNO)).thenReturn(Optional.of(student));
        when(assignments.findTopByStudentIdAndValidFromLessThanEqualOrderByValidFromDesc(7L, LocalDate.of(2026, 11, 1)))
                .thenReturn(Optional.of(new PlanAssignment(student, premium, LocalDate.of(2026, 11, 1))));
        when(fees.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        assertEquals(new BigDecimal("180.00"), feeService.create(7L, LocalDate.of(2026, 11, 1), UserRole.ADMIN).amount());
    }

    @Test void monthlyFeeIsBlockedWhenReferenceMonthHasNoPlan() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        when(users.findByIdAndRole(7L, UserRole.ALUNO)).thenReturn(Optional.of(student));
        when(assignments.findTopByStudentIdAndValidFromLessThanEqualOrderByValidFromDesc(7L, LocalDate.of(2026, 10, 1)))
                .thenReturn(Optional.empty());

        assertThrows(MonthlyFeeService.MissingPlanException.class,
                () -> feeService.create(7L, LocalDate.of(2026, 10, 1), UserRole.ADMIN));
        verify(fees, never()).saveAndFlush(any());
    }

    @Test void duplicatePlanChangeIsRejectedWithoutCreatingHistory() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        when(users.findByIdAndRole(7L, UserRole.ALUNO)).thenReturn(Optional.of(student));
        when(plans.findById(2L)).thenReturn(Optional.of(new Plan("Premium", new BigDecimal("180.00"))));
        when(assignments.existsByStudentIdAndValidFrom(7L, LocalDate.of(2026, 11, 1))).thenReturn(true);

        assertThrows(PlanService.DuplicatePlanAssignmentException.class,
                () -> planService.associate(7L, 2L, LocalDate.of(2026, 11, 1), UserRole.ADMIN));
        verify(assignments, never()).saveAndFlush(any());
    }

    @Test void existingMonthlyFeeKeepsItsHistoricalAmountAfterPlanChange() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        MonthlyFee fee = new MonthlyFee(student, LocalDate.of(2026, 10, 1), new BigDecimal("100.00"));
        student.setPlan(new Plan("Premium", new BigDecimal("180.00")));

        assertEquals(new BigDecimal("100.00"), fee.getAmount());
    }

    @Test void changingStudentDueDayDoesNotChangeAnExistingFee() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        MonthlyFee fee = new MonthlyFee(student, LocalDate.of(2026, 10, 1), new BigDecimal("120.00"));
        student.setDueDay(18);

        assertEquals(LocalDate.of(2026, 10, 10), fee.getDueDate());
    }

    @Test void receivingActivatesStudentAndCancellingReturnsToPending() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        MonthlyFee fee = new MonthlyFee(student, LocalDate.of(2026, 9, 1), new BigDecimal("120.00"));
        when(fees.findById(1L)).thenReturn(Optional.of(fee));
        assertEquals("paga", feeService.receive(1L, UserRole.ADMIN).status());
        assertEquals("ativo", student.getStatus());
        assertEquals("pendente", feeService.cancel(1L, UserRole.ADMIN).status());
        assertThrows(MonthlyFeeService.InvalidStatusException.class, () -> feeService.cancel(1L, UserRole.ADMIN));
    }

    @Test void partialPaymentKeepsFeePendingAndReportsTotalPaid() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        MonthlyFee fee = new MonthlyFee(student, LocalDate.of(2026, 9, 1), new BigDecimal("200.00"));
        when(fees.findById(1L)).thenReturn(Optional.of(fee));
        when(payments.findAllByMonthlyFeeId(1L)).thenReturn(List.of());
        when(payments.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        var result = feeService.receive(1L, new BigDecimal("80.00"), UserRole.ADMIN);

        assertEquals("pendente", result.status());
        assertEquals(new BigDecimal("80.00"), result.totalPaid());
        assertEquals("inativo", student.getStatus());
    }

    @Test void feeIsPaidWhenPartialPaymentsReachTheAmount() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        MonthlyFee fee = new MonthlyFee(student, LocalDate.of(2026, 9, 1), new BigDecimal("200.00"));
        MonthlyFeePayment previous = new MonthlyFeePayment(fee, new BigDecimal("80.00"));
        when(fees.findById(1L)).thenReturn(Optional.of(fee));
        when(payments.findAllByMonthlyFeeId(1L)).thenReturn(List.of(previous));
        when(payments.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        var result = feeService.receive(1L, new BigDecimal("120.00"), UserRole.ADMIN);

        assertEquals("paga", result.status());
        assertEquals(new BigDecimal("200.00"), result.totalPaid());
        assertEquals("ativo", student.getStatus());
    }

    @Test void inactiveStudentIsActivatedOnlyAfterRemainingPayment() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        MonthlyFee fee = new MonthlyFee(student, LocalDate.of(2026, 9, 1), new BigDecimal("200.00"));
        when(fees.findById(1L)).thenReturn(Optional.of(fee));
        when(payments.findAllByMonthlyFeeId(1L)).thenReturn(List.of(), List.of(new MonthlyFeePayment(fee, new BigDecimal("100.00"))));
        when(payments.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        feeService.receive(1L, new BigDecimal("100.00"), UserRole.ADMIN);
        assertEquals("inativo", student.getStatus());

        var result = feeService.receive(1L, new BigDecimal("100.00"), UserRole.ADMIN);
        assertEquals("paga", result.status());
        assertEquals("ativo", student.getStatus());
    }

    @Test void paymentAboveRemainingBalanceIsRejected() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        MonthlyFee fee = new MonthlyFee(student, LocalDate.of(2026, 9, 1), new BigDecimal("200.00"));
        MonthlyFeePayment previous = new MonthlyFeePayment(fee, new BigDecimal("150.00"));
        when(fees.findById(1L)).thenReturn(Optional.of(fee));
        when(payments.findAllByMonthlyFeeId(1L)).thenReturn(List.of(previous));

        assertThrows(MonthlyFeeService.PaymentExceedsBalanceException.class,
                () -> feeService.receive(1L, new BigDecimal("60.00"), UserRole.ADMIN));
        verify(payments, never()).saveAndFlush(any());
    }

    @Test void cancellingOnePaymentRecalculatesFeeStatusAndTotal() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        MonthlyFee fee = new MonthlyFee(student, LocalDate.of(2026, 9, 1), new BigDecimal("200.00"));
        MonthlyFeePayment cancelled = new MonthlyFeePayment(fee, new BigDecimal("80.00"));
        MonthlyFeePayment remaining = new MonthlyFeePayment(fee, new BigDecimal("120.00"));
        when(payments.findById(1L)).thenReturn(Optional.of(cancelled));
        when(payments.findAllByMonthlyFeeId(null)).thenReturn(List.of(cancelled, remaining));

        var result = feeService.cancelPayment(1L, UserRole.ADMIN);

        assertEquals("pendente", result.status());
        assertEquals(new BigDecimal("120.00"), result.totalPaid());
        assertEquals("cancelado", cancelled.getStatus());
    }
}
