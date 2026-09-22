package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.enrollment.EnrollmentService;
import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFee;
import devheloisacabral.tcc.com.esportiva.domain.professor.Professor;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.enrollment.EnrollmentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.turma.TurmaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {
    @Mock EnrollmentRepository enrollments;
    @Mock AuthUserRepository users;
    @Mock MonthlyFeeRepository fees;
    @Mock TurmaRepository turmas;
    private EnrollmentService service;
    private AuthUser activeStudent;
    private Turma activeTurma;

    @BeforeEach
    void setUp() throws Exception {
        service = new EnrollmentService(enrollments, users, fees, turmas);
        activeStudent = student(20L, "Ana");
        activeStudent.activate();
        activeTurma = turma(30L, "Vôlei", "ativa", 2);
    }

    @Test
    void adminEnrollsActiveStudentInActiveTurmaWithAvailableCapacityAndStoresDate() {
        when(users.findByIdAndRole(20L, UserRole.ALUNO)).thenReturn(Optional.of(activeStudent));
        when(turmas.findByIdForEnrollment(30L)).thenReturn(Optional.of(activeTurma));
        when(enrollments.countByTurmaId(30L)).thenReturn(0L);
        when(fees.existsByStudentIdAndStatusAndDueDateBefore(anyLong(), anyString(), any(LocalDate.class))).thenReturn(false);
        when(enrollments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(20L, 30L, UserRole.ADMIN);

        assertEquals(20L, result.aluno().id());
        assertEquals(30L, result.turma().id());
        assertEquals("Ana", result.aluno().name());
        assertEquals("ativa", result.turma().status());
        org.junit.jupiter.api.Assertions.assertNotNull(result.enrolledAt());
    }

    @Test
    void rejectsInactiveStudentAndDoesNotCreateEnrollment() throws Exception {
        AuthUser inactiveStudent = student(20L, "Ana");
        when(users.findByIdAndRole(20L, UserRole.ALUNO)).thenReturn(Optional.of(inactiveStudent));
        when(turmas.findByIdForEnrollment(30L)).thenReturn(Optional.of(activeTurma));

        assertThrows(EnrollmentService.InvalidEnrollmentException.class, () -> service.create(20L, 30L, UserRole.ADMIN));
        verify(enrollments, never()).save(any());
    }

    @Test
    void rejectsInactiveTurmaAndDoesNotCreateEnrollment() throws Exception {
        Turma inactiveTurma = turma(30L, "Vôlei", "inativa", 2);
        when(users.findByIdAndRole(20L, UserRole.ALUNO)).thenReturn(Optional.of(activeStudent));
        when(turmas.findByIdForEnrollment(30L)).thenReturn(Optional.of(inactiveTurma));

        assertThrows(EnrollmentService.InvalidEnrollmentException.class, () -> service.create(20L, 30L, UserRole.ADMIN));
        verify(enrollments, never()).save(any());
    }

    @Test
    void rejectsEnrollmentWhenTurmaCapacityIsReached() {
        when(users.findByIdAndRole(20L, UserRole.ALUNO)).thenReturn(Optional.of(activeStudent));
        when(turmas.findByIdForEnrollment(30L)).thenReturn(Optional.of(activeTurma));
        when(enrollments.countByTurmaId(30L)).thenReturn(2L);

        assertThrows(EnrollmentService.InvalidEnrollmentException.class, () -> service.create(20L, 30L, UserRole.ADMIN));
        verify(enrollments, never()).save(any());
    }

    @Test
    void rejectsStudentWithOverduePendingMonthlyFeeWithoutCreatingEnrollment() {
        when(users.findByIdAndRole(20L, UserRole.ALUNO)).thenReturn(Optional.of(activeStudent));
        when(turmas.findByIdForEnrollment(30L)).thenReturn(Optional.of(activeTurma));
        when(fees.existsByStudentIdAndStatusAndDueDateBefore(anyLong(), anyString(), any(LocalDate.class))).thenReturn(true);

        assertThrows(EnrollmentService.OverdueMonthlyFeeException.class, () -> service.create(20L, 30L, UserRole.ADMIN));
        verify(enrollments, never()).countByTurmaId(anyLong());
        verify(enrollments, never()).save(any());
    }

    @Test
    void allowsStudentWithFuturePendingMonthlyFee() throws Exception {
        when(users.findByIdAndRole(20L, UserRole.ALUNO)).thenReturn(Optional.of(activeStudent));
        when(turmas.findByIdForEnrollment(30L)).thenReturn(Optional.of(activeTurma));
        when(fees.existsByStudentIdAndStatusAndDueDateBefore(anyLong(), anyString(), any(LocalDate.class))).thenReturn(false);
        when(enrollments.countByTurmaId(30L)).thenReturn(0L);
        when(enrollments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(30L, service.create(20L, 30L, UserRole.ADMIN).turma().id());
        verify(enrollments).save(any());
    }

    @Test
    void receivingOnlyOverdueMonthlyFeeRemovesOperationalBlock() throws Exception {
        MonthlyFee fee = new MonthlyFee(activeStudent, LocalDate.now().minusMonths(1).withDayOfMonth(1), BigDecimal.TEN);
        when(fees.findById(1L)).thenReturn(Optional.of(fee));
        when(users.findByIdAndRole(20L, UserRole.ALUNO)).thenReturn(Optional.of(activeStudent));
        when(fees.existsByStudentIdAndStatusAndDueDateBefore(anyLong(), anyString(), any(LocalDate.class)))
                .thenAnswer(invocation -> "pendente".equals(fee.getStatus()));
        when(turmas.findByIdForEnrollment(30L)).thenReturn(Optional.of(activeTurma));
        when(enrollments.countByTurmaId(30L)).thenReturn(0L);
        when(enrollments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MonthlyFeeService feeService = new MonthlyFeeService(fees, users);
        assertEquals("paga", feeService.receive(1L, UserRole.ADMIN).status());
        assertEquals(30L, service.create(20L, 30L, UserRole.ADMIN).turma().id());
        verify(enrollments).save(any());
    }

    @Test
    void allowsSameStudentInAnotherTurmaWithoutRemovingPreviousEnrollment() throws Exception {
        Turma anotherTurma = turma(31L, "Basquete", "ativa", 2);
        when(users.findByIdAndRole(20L, UserRole.ALUNO)).thenReturn(Optional.of(activeStudent));
        when(turmas.findByIdForEnrollment(31L)).thenReturn(Optional.of(anotherTurma));
        when(enrollments.countByTurmaId(31L)).thenReturn(0L);
        when(enrollments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(20L, 31L, UserRole.ADMIN);

        assertEquals(31L, result.turma().id());
        verify(enrollments).save(any());
    }

    @Test
    void adminQueriesAllEnrollmentsAndAlunoQueriesOnlyOwnEnrollments() {
        when(enrollments.findAllWithStudentAndTurma()).thenReturn(List.of());
        when(enrollments.findAllWithStudentAndTurmaByStudentId(20L)).thenReturn(List.of());

        assertEquals(0, service.findAll(UserRole.ADMIN, 1L).size());
        assertEquals(0, service.findAll(UserRole.ALUNO, 20L).size());
        verify(enrollments).findAllWithStudentAndTurma();
        verify(enrollments).findAllWithStudentAndTurmaByStudentId(20L);
    }

    @Test
    void professorCannotManageOrConsultEnrollments() {
        assertThrows(AccessDeniedException.class, () -> service.create(20L, 30L, UserRole.PROFESSOR));
        assertThrows(AccessDeniedException.class, () -> service.findAll(UserRole.PROFESSOR, 40L));
    }

    @Test
    void alunoCannotCreateEnrollment() {
        assertThrows(AccessDeniedException.class, () -> service.create(20L, 30L, UserRole.ALUNO));
    }

    private AuthUser student(long id, String name) throws Exception {
        AuthUser student = new AuthUser(name, name.toLowerCase() + "@example.com", "11999999999",
                LocalDate.of(2000, 1, 1), "hash");
        setId(student, id);
        return student;
    }

    private Turma turma(long id, String name, String status, int capacity) throws Exception {
        AuthUser professorUser = new AuthUser("Professor", "professor" + id + "@example.com",
                LocalDate.of(1980, 1, 1), "hash", UserRole.PROFESSOR);
        setId(professorUser, id + 100);
        Professor professor = new Professor(professorUser, "CREF-" + id);
        setId(professor, id + 200);
        Turma turma = new Turma(name, "segunda-feira", LocalTime.of(18, 0), LocalTime.of(19, 0), capacity,
                status, new LinkedHashSet<>(List.of(professor)), professor);
        setId(turma, id);
        return turma;
    }

    private void setId(Object target, long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
