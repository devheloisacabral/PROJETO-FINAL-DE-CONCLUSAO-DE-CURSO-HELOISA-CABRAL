package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.dashboard.DashboardService;
import devheloisacabral.tcc.com.esportiva.domain.aula.Aula;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import devheloisacabral.tcc.com.esportiva.persistence.aula.AulaRepository;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.enrollment.EnrollmentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.professor.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock AuthUserRepository users;
    @Mock ProfessorRepository professors;
    @Mock EnrollmentRepository enrollments;
    @Mock MonthlyFeeRepository monthlyFees;
    @Mock AulaRepository aulas;
    private DashboardService service;

    @BeforeEach
    void setUp() { service = new DashboardService(users, professors, enrollments, monthlyFees, aulas); }

    @Test
    void calculatesIndicatorsAndMapsUpcomingLessonsWithMaximumFive() throws Exception {
        Turma turma = mock(Turma.class);
        when(turma.getId()).thenReturn(12L);
        when(turma.getName()).thenReturn("Turma Adulto");
        Aula aula = mock(Aula.class);
        when(aula.getId()).thenReturn(80L);
        when(aula.getTurma()).thenReturn(turma);
        when(aula.getDate()).thenReturn(LocalDate.of(2026, 9, 25));
        when(aula.getStartTime()).thenReturn(LocalTime.of(18, 0));
        when(aula.getEndTime()).thenReturn(LocalTime.of(19, 0));
        when(aula.isCancelled()).thenReturn(false);
        when(aulas.findUpcomingWithTurma(any(), any(), any(Pageable.class))).thenReturn(List.of(aula));
        when(users.countByRole(UserRole.ALUNO)).thenReturn(7L);
        when(professors.count()).thenReturn(3L);
        when(enrollments.countActiveEnrollments()).thenReturn(5L);
        when(monthlyFees.countByReferenceMonthAndStatus(LocalDate.of(2026, 9, 1), "pendente")).thenReturn(4L);

        var result = service.find("2026-09", UserRole.ADMIN);

        assertEquals("2026-09", result.mesReferencia());
        assertEquals(7L, result.totalAlunos());
        assertEquals(3L, result.totalProfessores());
        assertEquals(5L, result.matriculasAtivas());
        assertEquals(4L, result.mensalidadesPendentes());
        assertEquals(1, result.proximasAulas().size());
        var lesson = result.proximasAulas().get(0);
        assertEquals(80L, lesson.id());
        assertEquals(LocalDate.of(2026, 9, 25), lesson.data());
        assertEquals(LocalTime.of(18, 0), lesson.horarioInicio());
        assertEquals(LocalTime.of(19, 0), lesson.horarioFim());
        assertEquals(12L, lesson.turmaId());
        assertEquals("Turma Adulto", lesson.turmaNome());
        assertFalse(lesson.cancelada());
        verify(aulas).findUpcomingWithTurma(any(), any(), argThat(page -> page.getPageSize() == 5));
    }

    @Test
    void returnsEmptyUpcomingLessonsWhenThereAreNone() {
        when(aulas.findUpcomingWithTurma(any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(users.countByRole(UserRole.ALUNO)).thenReturn(0L);
        when(professors.count()).thenReturn(0L);
        when(enrollments.countActiveEnrollments()).thenReturn(0L);
        when(monthlyFees.countByReferenceMonthAndStatus(any(), eq("pendente"))).thenReturn(0L);

        assertTrue(service.find("2026-09", UserRole.ADMIN).proximasAulas().isEmpty());
    }

    @Test
    void dashboardUsesRescheduledDateAndDoesNotExposeOldDate() {
        Aula aula = mock(Aula.class);
        Turma turma = mock(Turma.class);
        when(aula.getId()).thenReturn(80L);
        when(aula.getTurma()).thenReturn(turma);
        when(turma.getId()).thenReturn(12L);
        when(turma.getName()).thenReturn("Turma Adulto");
        when(aula.getDate()).thenReturn(LocalDate.of(2026, 10, 5));
        when(aula.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(aula.getEndTime()).thenReturn(LocalTime.of(10, 0));
        when(aula.isCancelled()).thenReturn(false);
        when(aulas.findUpcomingWithTurma(any(), any(), any(Pageable.class))).thenReturn(List.of(aula));
        when(users.countByRole(UserRole.ALUNO)).thenReturn(0L);
        when(professors.count()).thenReturn(0L);
        when(enrollments.countActiveEnrollments()).thenReturn(0L);
        when(monthlyFees.countByReferenceMonthAndStatus(any(), eq("pendente"))).thenReturn(0L);

        var result = service.find("2026-10", UserRole.ADMIN);

        assertEquals(List.of(LocalDate.of(2026, 10, 5)), result.proximasAulas().stream().map(DashboardService.UpcomingLesson::data).toList());
        assertFalse(result.proximasAulas().stream().anyMatch(lesson -> lesson.data().equals(LocalDate.of(2026, 10, 1))));
    }

    @Test
    void rejectsNonAdminAndInvalidReferenceMonth() {
        assertThrows(AccessDeniedException.class, () -> service.find("2026-09", UserRole.PROFESSOR));
        assertThrows(DashboardService.InvalidMonthException.class, () -> service.find("09-2026", UserRole.ADMIN));
        verifyNoInteractions(users, professors, enrollments, monthlyFees, aulas);
    }
}
