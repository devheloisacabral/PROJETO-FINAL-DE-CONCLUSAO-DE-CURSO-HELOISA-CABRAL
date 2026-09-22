package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService;
import devheloisacabral.tcc.com.esportiva.application.plan.PlanService;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFee;
import devheloisacabral.tcc.com.esportiva.domain.plan.Plan;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.plan.PlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
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
        when(users.findByIdAndRole(7L, UserRole.ALUNO)).thenReturn(Optional.of(student));
        when(fees.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        var result = feeService.create(7L, LocalDate.of(2026, 9, 1), UserRole.ADMIN);
        assertEquals(new BigDecimal("120.00"), result.amount());
        assertEquals(LocalDate.of(2026, 9, 20), result.dueDate());
        assertEquals("pendente", result.status());
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
}
