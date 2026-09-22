package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.aula.AulaService;
import devheloisacabral.tcc.com.esportiva.domain.aula.Aula;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.professor.Professor;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import devheloisacabral.tcc.com.esportiva.persistence.aula.AulaRepository;
import devheloisacabral.tcc.com.esportiva.persistence.turma.TurmaRepository;
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
class AulaServiceTest {
    @Mock AulaRepository aulas;
    @Mock TurmaRepository turmas;
    private AulaService service;
    private Turma turma;

    @BeforeEach
    void setUp() throws Exception {
        service = new AulaService(aulas, turmas);
        turma = turma(30L, 40L);
    }

    @Test
    void adminGeneratesLessonOnConfiguredWeekdayWithTurmaTimesAndNotCancelled() {
        when(turmas.findById(30L)).thenReturn(Optional.of(turma));
        when(aulas.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(30L, LocalDate.of(2026, 9, 21), UserRole.ADMIN);

        assertEquals(30L, result.turma().id());
        assertEquals(LocalDate.of(2026, 9, 21), result.data());
        assertEquals(LocalTime.of(18, 0), result.horarioInicio());
        assertEquals(LocalTime.of(19, 0), result.horarioFim());
        assertFalse(result.cancelada());
    }

    @Test
    void rejectsLessonOnDifferentWeekday() {
        when(turmas.findById(30L)).thenReturn(Optional.of(turma));

        assertThrows(AulaService.WeekdayMismatchException.class,
                () -> service.create(30L, LocalDate.of(2026, 9, 22), UserRole.ADMIN));
        verify(aulas, never()).save(any());
    }

    @Test
    void rejectsMissingTurma() {
        when(turmas.findById(30L)).thenReturn(Optional.empty());
        assertThrows(AulaService.NotFoundException.class,
                () -> service.create(30L, LocalDate.of(2026, 9, 21), UserRole.ADMIN));
    }

    @Test
    void rejectsMissingDateOrInvalidTurmaTimes() throws Exception {
        assertThrows(AulaService.InvalidAulaException.class, () -> service.create(30L, null, UserRole.ADMIN));
        Turma invalid = turma(30L, 40L);
        setField(invalid, "startTime", LocalTime.of(19, 0));
        setField(invalid, "endTime", LocalTime.of(18, 0));
        when(turmas.findById(30L)).thenReturn(Optional.of(invalid));
        assertThrows(AulaService.InvalidAulaException.class,
                () -> service.create(30L, LocalDate.of(2026, 9, 21), UserRole.ADMIN));
    }

    @Test
    void adminProfessorAndStudentQueriesUseTheirAllowedLessons() throws Exception {
        Aula aula = new Aula(turma, LocalDate.of(2026, 9, 21), LocalTime.of(18, 0), LocalTime.of(19, 0));
        setId(aula, 70L);
        when(aulas.findAllWithTurma()).thenReturn(List.of(aula));
        when(aulas.findAllWithTurmaByProfessorUserId(40L)).thenReturn(List.of(aula));
        when(aulas.findAllWithTurmaByStudentUserId(50L)).thenReturn(List.of(aula));

        assertEquals(1, service.findAll(UserRole.ADMIN, 1L).size());
        assertEquals(1, service.findAll(UserRole.PROFESSOR, 40L).size());
        assertEquals(1, service.findAll(UserRole.ALUNO, 50L).size());
        verify(aulas).findAllWithTurma();
        verify(aulas).findAllWithTurmaByProfessorUserId(40L);
        verify(aulas).findAllWithTurmaByStudentUserId(50L);
    }

    @Test
    void authorizedUserCanQueryCancelledLesson() throws Exception {
        Aula aula = new Aula(turma, LocalDate.of(2026, 9, 21), LocalTime.of(18, 0), LocalTime.of(19, 0));
        setId(aula, 70L);
        setField(aula, "cancelled", true);
        when(aulas.findByIdWithTurma(70L)).thenReturn(Optional.of(aula));

        assertTrue(service.findOne(70L, UserRole.ADMIN, 1L).cancelada());
    }

    @Test
    void professorAndStudentCannotQueryUnassociatedLesson() throws Exception {
        Aula aula = new Aula(turma, LocalDate.of(2026, 9, 21), LocalTime.of(18, 0), LocalTime.of(19, 0));
        setId(aula, 70L);
        when(aulas.findByIdWithTurma(70L)).thenReturn(Optional.of(aula));
        when(aulas.findAllWithTurmaByStudentUserId(50L)).thenReturn(List.of());

        assertThrows(AccessDeniedException.class, () -> service.findOne(70L, UserRole.PROFESSOR, 999L));
        assertThrows(AccessDeniedException.class, () -> service.findOne(70L, UserRole.ALUNO, 50L));
    }

    @Test
    void onlyAdminCanGenerateLessons() {
        assertThrows(AccessDeniedException.class, () -> service.create(30L, LocalDate.of(2026, 9, 21), UserRole.PROFESSOR));
        assertThrows(AccessDeniedException.class, () -> service.create(30L, LocalDate.of(2026, 9, 21), UserRole.ALUNO));
        verifyNoInteractions(turmas, aulas);
    }

    private Turma turma(long turmaId, long professorUserId) throws Exception {
        AuthUser user = new AuthUser("Professor", "professor" + turmaId + "@example.com", LocalDate.of(1980, 1, 1), "hash", UserRole.PROFESSOR);
        setId(user, professorUserId);
        Professor professor = new Professor(user, "CREF-" + turmaId);
        setId(professor, turmaId + 200);
        Turma result = new Turma("Vôlei", "segunda-feira", LocalTime.of(18, 0), LocalTime.of(19, 0), 10,
                "ativa", new LinkedHashSet<>(List.of(professor)), professor);
        setId(result, turmaId);
        return result;
    }

    private void setId(Object target, long id) throws Exception { setField(target, "id", id); }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
