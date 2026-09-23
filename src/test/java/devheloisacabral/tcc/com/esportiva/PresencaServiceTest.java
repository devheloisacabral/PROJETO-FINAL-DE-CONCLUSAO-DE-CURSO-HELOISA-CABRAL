package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.presenca.PresencaService;
import devheloisacabral.tcc.com.esportiva.domain.aula.Aula;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.presenca.Presenca;
import devheloisacabral.tcc.com.esportiva.domain.professor.Professor;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import devheloisacabral.tcc.com.esportiva.persistence.aula.AulaRepository;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.presenca.PresencaRepository;
import devheloisacabral.tcc.com.esportiva.persistence.enrollment.EnrollmentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresencaServiceTest {
    @Mock PresencaRepository presencas;
    @Mock AuthUserRepository users;
    @Mock AulaRepository aulas;
    @Mock MonthlyFeeRepository fees;
    @Mock EnrollmentRepository enrollments;
    private PresencaService service;
    private AuthUser aluno;
    private Aula aula;
    private Turma turma;
    private Professor professor;

    @BeforeEach
    void setUp() throws Exception {
        service = new PresencaService(presencas, users, fees, aulas, enrollments);
        aluno = student(50L, "ativo");
        AuthUser professorUser = new AuthUser("Professor", "p@example.com", LocalDate.of(1980, 1, 1), "hash", UserRole.PROFESSOR);
        setId(professorUser, 40L);
        professor = new Professor(professorUser, "CREF-40");
        setId(professor, 140L);
        turma = new Turma("Vôlei", "segunda-feira", LocalTime.of(18, 0), LocalTime.of(19, 0), 10, "ativa",
                new LinkedHashSet<>(List.of(professor)), professor);
        setId(turma, 30L);
        aula = new Aula(turma, LocalDate.of(2026, 9, 21), LocalTime.of(18, 0), LocalTime.of(19, 0));
        setId(aula, 70L);
        lenient().when(users.findByIdAndRole(50L, UserRole.ALUNO)).thenReturn(Optional.of(aluno));
        lenient().when(aulas.findByIdWithTurma(70L)).thenReturn(Optional.of(aula));
        lenient().when(presencas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(enrollments.existsByStudentIdAndTurmaIdAndStatus(50L, 30L, "ativa")).thenReturn(true);
    }

    @Test
    void adminRegistersActiveStudentAndProfessorRegistersInAssociatedClass() {
        when(fees.existsByStudentIdAndStatusAndDueDateBefore(anyLong(), anyString(), any(LocalDate.class))).thenReturn(false);
        assertNotNull(service.create(50L, 70L, UserRole.ADMIN, 1L));
        assertNotNull(service.create(50L, 70L, UserRole.PROFESSOR, 40L));
        verify(presencas, times(2)).save(any(Presenca.class));
    }

    @Test
    void adminAndProfessorCannotRegisterInactiveStudent() throws Exception {
        setField(aluno, "status", "inativo");
        assertThrows(PresencaService.InactiveStudentException.class, () -> service.create(50L, 70L, UserRole.ADMIN, 1L));
        assertThrows(PresencaService.InactiveStudentException.class, () -> service.create(50L, 70L, UserRole.PROFESSOR, 40L));
        verify(presencas, never()).save(any());
    }

    @Test
    void blocksPresenceForStudentWithOverduePendingMonthlyFeeWithoutSaving() {
        when(fees.existsByStudentIdAndStatusAndDueDateBefore(anyLong(), anyString(), any(LocalDate.class))).thenReturn(true);

        assertThrows(PresencaService.OverdueMonthlyFeeException.class,
                () -> service.create(50L, 70L, UserRole.PROFESSOR, 40L));
        verify(presencas, never()).save(any());
    }

    @Test
    void professorCannotRegisterInUnassociatedClass() {
        assertThrows(AccessDeniedException.class, () -> service.create(50L, 70L, UserRole.PROFESSOR, 999L));
        verify(presencas, never()).save(any());
    }

    @Test
    void cancelledEnrollmentBlocksFuturePresenceWithoutDeletingHistory() {
        when(enrollments.existsByStudentIdAndTurmaIdAndStatus(50L, 30L, "ativa")).thenReturn(false);

        assertThrows(PresencaService.NotEnrolledException.class,
                () -> service.create(50L, 70L, UserRole.PROFESSOR, 40L));
        verify(presencas, never()).save(any());

        Presenca oldPresence = new Presenca(aluno, aula, LocalDateTime.now().minusDays(1));
        when(presencas.findAllWithDetailsByStudentId(50L)).thenReturn(List.of(oldPresence));
        assertEquals(1, service.findAll(UserRole.ALUNO, 50L, null).size());
    }

    @Test
    void studentCannotRegisterPresence() {
        assertThrows(AccessDeniedException.class, () -> service.create(50L, 70L, UserRole.ALUNO, 50L));
        verifyNoInteractions(users, aulas, presencas);
    }

    @Test
    void adminSeesAllAndProfessorSeesOnlyAssociatedClasses() {
        Presenca presence = new Presenca(aluno, aula, LocalDateTime.now());
        when(presencas.findAllWithDetails()).thenReturn(List.of(presence));
        when(presencas.findAllWithDetailsByProfessorUserId(40L)).thenReturn(List.of(presence));
        assertEquals(1, service.findAll(UserRole.ADMIN, 1L, null).size());
        assertEquals(1, service.findAll(UserRole.PROFESSOR, 40L, null).size());
        verify(presencas).findAllWithDetails();
        verify(presencas).findAllWithDetailsByProfessorUserId(40L);
    }

    @Test
    void studentSeesOnlyOwnHistoryAndCannotRequestAnotherStudent() {
        when(presencas.findAllWithDetailsByStudentId(50L)).thenReturn(List.of(new Presenca(aluno, aula, LocalDateTime.now())));
        assertEquals(1, service.findAll(UserRole.ALUNO, 50L, null).size());
        assertThrows(AccessDeniedException.class, () -> service.findAll(UserRole.ALUNO, 50L, 999L));
        verify(presencas).findAllWithDetailsByStudentId(50L);
    }

    private AuthUser student(long id, String status) throws Exception {
        AuthUser result = new AuthUser("Aluno", "a" + id + "@example.com", "11999999999", LocalDate.of(2000, 1, 1), "hash");
        setId(result, id);
        setField(result, "status", status);
        return result;
    }

    private void setId(Object target, long id) throws Exception { setField(target, "id", id); }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
