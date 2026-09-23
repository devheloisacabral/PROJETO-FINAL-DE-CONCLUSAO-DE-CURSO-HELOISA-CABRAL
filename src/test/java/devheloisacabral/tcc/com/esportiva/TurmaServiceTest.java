package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.turma.TurmaService;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.professor.Professor;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import devheloisacabral.tcc.com.esportiva.persistence.professor.ProfessorRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurmaServiceTest {
    @Mock TurmaRepository turmas;
    @Mock ProfessorRepository professors;
    private TurmaService service;
    private Professor professor;
    private Professor secondProfessor;

    @BeforeEach
    void setUp() throws Exception {
        service = new TurmaService(turmas, professors);
        professor = professor(10L, 100L, "Ana");
        secondProfessor = professor(11L, 101L, "Bia");
    }

    @Test
    void createsActiveTurmaWithAssociationsAndOneResponsibleProfessor() {
        when(professors.findAllById(List.of(10L, 11L))).thenReturn(List.of(professor, secondProfessor));
        when(turmas.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(" Vôlei ", "segunda-feira", LocalTime.of(18, 0), LocalTime.of(19, 30),
                20, "ativa", List.of(10L, 11L), List.of(11L), UserRole.ADMIN);

        assertEquals("Vôlei", result.name());
        assertEquals("ativa", result.status());
        assertEquals(2, result.professors().size());
        assertEquals(11L, result.responsibleProfessor().id());
    }

    @Test
    void blocksProfessorInOverlappingActiveTurma() {
        when(professors.findAllById(List.of(10L))).thenReturn(List.of(professor));
        when(turmas.existsActiveScheduleConflict("segunda-feira", LocalTime.of(9, 30), LocalTime.of(10, 30), List.of(10L), null))
                .thenReturn(true);

        assertThrows(TurmaService.InvalidTurmaException.class, () -> service.create("Vôlei", "segunda-feira",
                LocalTime.of(9, 30), LocalTime.of(10, 30), 10, "ativa", List.of(10L), List.of(10L), UserRole.ADMIN));
        verify(turmas, never()).save(any());
    }

    @Test
    void allowsProfessorTurmasThatOnlyTouch() {
        when(professors.findAllById(List.of(10L))).thenReturn(List.of(professor));
        when(turmas.existsActiveScheduleConflict("segunda-feira", LocalTime.of(10, 0), LocalTime.of(11, 0), List.of(10L), null))
                .thenReturn(false);
        when(turmas.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("Vôlei", service.create("Vôlei", "segunda-feira", LocalTime.of(10, 0), LocalTime.of(11, 0),
                10, "ativa", List.of(10L), List.of(10L), UserRole.ADMIN).name());
    }

    @Test
    void createsInactiveTurmaWithValidData() {
        when(professors.findAllById(List.of(10L))).thenReturn(List.of(professor));
        when(turmas.save(any(Turma.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create("Vôlei", "terça-feira", LocalTime.of(18, 0), LocalTime.of(19, 0),
                15, "inativa", List.of(10L), List.of(10L), UserRole.ADMIN);

        assertEquals("inativa", result.status());
    }

    @Test
    void rejectsMissingAssociationsMissingResponsibleOutsideAssociationsAndMultipleResponsible() {
        assertInvalid(List.of(), List.of());
        assertInvalid(List.of(10L), List.of());
        assertInvalid(List.of(10L), List.of(11L));
        assertInvalid(List.of(10L, 11L), List.of(10L, 11L));
        verify(turmas, never()).save(any());
    }

    @Test
    void rejectsInvalidCapacityScheduleStatusAndInactiveProfessor() {
        assertInvalid(List.of(10L), List.of(10L), 0, LocalTime.of(18, 0), LocalTime.of(19, 0), "ativa");
        assertInvalid(List.of(10L), List.of(10L), 10, LocalTime.of(19, 0), LocalTime.of(19, 0), "ativa");
        assertInvalid(List.of(10L), List.of(10L), 10, LocalTime.of(19, 0), LocalTime.of(20, 0), "pausada");
        professor.inactivate();
        when(professors.findAllById(List.of(10L))).thenReturn(List.of(professor));
        assertInvalid(List.of(10L), List.of(10L));
    }

    @Test
    void allowsAdminReadUpdateStatusDeleteAndRejectsNonAdminWrites() {
        when(professors.findAllById(List.of(10L))).thenReturn(List.of(professor));
        Turma created = new Turma("Vôlei", "segunda-feira", LocalTime.of(18, 0), LocalTime.of(19, 0), 10, "ativa",
                new java.util.LinkedHashSet<>(List.of(professor)), professor);
        when(turmas.findById(1L)).thenReturn(Optional.of(created));

        assertEquals("Vôlei", service.findOne(1L, UserRole.ADMIN, 1L).name());
        service.update(1L, "Vôlei A", "terça-feira", LocalTime.of(19, 0), LocalTime.of(20, 0), 12, "inativa", List.of(10L), List.of(10L), UserRole.ADMIN);
        assertEquals("inativa", created.getStatus());
        service.reactivate(1L, UserRole.ADMIN);
        service.inactivate(1L, UserRole.ADMIN);
        service.delete(1L, UserRole.ADMIN);
        assertThrows(AccessDeniedException.class, () -> service.create("x", "segunda", LocalTime.NOON, LocalTime.of(13, 0), 1, "ativa", List.of(10L), List.of(10L), UserRole.PROFESSOR));
        assertThrows(AccessDeniedException.class, () -> service.update(1L, "x", "segunda", LocalTime.NOON, LocalTime.of(13, 0), 1, "ativa", List.of(10L), List.of(10L), UserRole.ALUNO));
        assertThrows(AccessDeniedException.class, () -> service.delete(1L, UserRole.ALUNO));
        verify(turmas).delete(created);
    }

    @Test
    void updatingTurmaBlocksWhenItsStudentsWouldOverlapAnotherActiveTurma() {
        when(professors.findAllById(List.of(10L))).thenReturn(List.of(professor));
        Turma created = new Turma("Vôlei", "segunda-feira", LocalTime.of(9, 0), LocalTime.of(10, 0), 10, "ativa",
                new java.util.LinkedHashSet<>(List.of(professor)), professor);
        when(turmas.findById(1L)).thenReturn(Optional.of(created));
        when(turmas.existsActiveEnrollmentScheduleConflict(1L, "segunda-feira", LocalTime.of(9, 30), LocalTime.of(10, 30)))
                .thenReturn(true);

        assertThrows(TurmaService.InvalidTurmaException.class, () -> service.update(1L, "Vôlei", "segunda-feira",
                LocalTime.of(9, 30), LocalTime.of(10, 30), 10, "ativa", List.of(10L), List.of(10L), UserRole.ADMIN));
        assertEquals(LocalTime.of(9, 0), created.getStartTime());
    }

    @Test
    void professorReadsOnlyAssociatedTurmasAndCannotReadUnassociatedTurma() throws Exception {
        Turma turma = new Turma("Vôlei", "segunda-feira", LocalTime.of(18, 0), LocalTime.of(19, 0), 10, "ativa",
                new java.util.LinkedHashSet<>(List.of(professor)), professor);
        when(turmas.findById(1L)).thenReturn(Optional.of(turma));
        when(professors.findByUser_Id(100L)).thenReturn(Optional.of(professor));
        when(turmas.findAllByProfessors_IdOrderByIdAsc(10L)).thenReturn(List.of(turma));

        assertEquals(1, service.findAll(UserRole.PROFESSOR, 100L).size());
        assertEquals("Vôlei", service.findOne(1L, UserRole.PROFESSOR, 100L).name());
        assertThrows(AccessDeniedException.class, () -> service.findOne(1L, UserRole.PROFESSOR, 999L));
        assertThrows(AccessDeniedException.class, () -> service.findAll(UserRole.ALUNO, 200L));
    }

    private void assertInvalid(List<Long> professorIds, List<Long> responsibleIds) {
        assertThrows(TurmaService.InvalidTurmaException.class, () -> service.create("Vôlei", "segunda-feira", LocalTime.of(18, 0), LocalTime.of(19, 0),
                10, "ativa", professorIds, responsibleIds, UserRole.ADMIN));
    }

    private void assertInvalid(List<Long> professorIds, List<Long> responsibleIds, int capacity, LocalTime start, LocalTime end, String status) {
        assertThrows(TurmaService.InvalidTurmaException.class, () -> service.create("Vôlei", "segunda-feira", start, end,
                capacity, status, professorIds, responsibleIds, UserRole.ADMIN));
    }

    private Professor professor(long id, long userId, String name) throws Exception {
        AuthUser user = new AuthUser(name, name.toLowerCase() + "@example.com", LocalDate.of(1990, 1, 1), "hash", UserRole.PROFESSOR);
        setId(user, userId);
        Professor value = new Professor(user, "CREF-" + id);
        setId(value, id);
        return value;
    }

    private void setId(Object target, long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
