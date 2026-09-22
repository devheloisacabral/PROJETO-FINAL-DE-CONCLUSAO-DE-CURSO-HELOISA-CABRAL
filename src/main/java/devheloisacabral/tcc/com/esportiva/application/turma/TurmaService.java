package devheloisacabral.tcc.com.esportiva.application.turma;

import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.professor.Professor;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import devheloisacabral.tcc.com.esportiva.persistence.professor.ProfessorRepository;
import devheloisacabral.tcc.com.esportiva.persistence.turma.TurmaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Objects;

@Service
public class TurmaService {
    private final TurmaRepository turmas;
    private final ProfessorRepository professors;

    public TurmaService(TurmaRepository turmas, ProfessorRepository professors) {
        this.turmas = turmas;
        this.professors = professors;
    }

    @Transactional
    public TurmaResult create(String name, String weekday, LocalTime startTime, LocalTime endTime,
                              Integer maxCapacity, String status, List<Long> professorIds,
                              List<Long> responsibleProfessorIds, UserRole role) {
        requireAdmin(role);
        Selection selection = validate(name, weekday, startTime, endTime, maxCapacity, status, professorIds, responsibleProfessorIds);
        return result(turmas.save(new Turma(name.trim(), weekday.trim(), startTime, endTime, maxCapacity,
                status, selection.professors(), selection.responsible())));
    }

    @Transactional(readOnly = true)
    public TurmaResult findOne(Long id, UserRole role, Long authenticatedUserId) {
        Turma turma = turmas.findById(id).orElseThrow(NotFoundException::new);
        if (role == UserRole.ADMIN) return result(turma);
        if (role != UserRole.PROFESSOR || authenticatedUserId == null || turma.getProfessors().stream()
                .noneMatch(professor -> Objects.equals(professor.getUser().getId(), authenticatedUserId))) {
            throw new AccessDeniedException("turma access is not authorized");
        }
        return result(turma);
    }

    @Transactional(readOnly = true)
    public List<TurmaResult> findAll(UserRole role, Long authenticatedUserId) {
        if (role == UserRole.ADMIN) return turmas.findAllByOrderByIdAsc().stream().map(this::result).toList();
        if (role != UserRole.PROFESSOR || authenticatedUserId == null) throw new AccessDeniedException("turma access is not authorized");
        return professors.findByUser_Id(authenticatedUserId).map(Professor::getId)
                .map(turmas::findAllByProfessors_IdOrderByIdAsc).orElseThrow(() -> new AccessDeniedException("turma access is not authorized"))
                .stream().map(this::result).toList();
    }

    @Transactional
    public TurmaResult update(Long id, String name, String weekday, LocalTime startTime, LocalTime endTime,
                              Integer maxCapacity, String status, List<Long> professorIds,
                              List<Long> responsibleProfessorIds, UserRole role) {
        requireAdmin(role);
        Turma turma = turmas.findById(id).orElseThrow(NotFoundException::new);
        Selection selection = validate(name, weekday, startTime, endTime, maxCapacity, status, professorIds, responsibleProfessorIds);
        turma.update(name.trim(), weekday.trim(), startTime, endTime, maxCapacity, status, selection.professors(), selection.responsible());
        return result(turma);
    }

    @Transactional
    public void inactivate(Long id, UserRole role) { changeStatus(id, role, false); }
    @Transactional
    public void reactivate(Long id, UserRole role) { changeStatus(id, role, true); }

    @Transactional
    public void delete(Long id, UserRole role) {
        requireAdmin(role);
        turmas.delete(turmas.findById(id).orElseThrow(NotFoundException::new));
    }

    private void changeStatus(Long id, UserRole role, boolean active) {
        requireAdmin(role);
        Turma turma = turmas.findById(id).orElseThrow(NotFoundException::new);
        if (active) turma.reactivate(); else turma.inactivate();
    }

    private Selection validate(String name, String weekday, LocalTime startTime, LocalTime endTime,
                               Integer maxCapacity, String status, List<Long> professorIds,
                               List<Long> responsibleProfessorIds) {
        if (name == null || name.isBlank() || weekday == null || weekday.isBlank() || startTime == null || endTime == null
                || maxCapacity == null || maxCapacity <= 0 || startTime.compareTo(endTime) >= 0
                || !("ativa".equals(status) || "inativa".equals(status))) throw new InvalidTurmaException();
        if (professorIds == null || professorIds.isEmpty() || new HashSet<>(professorIds).size() != professorIds.size()
                || responsibleProfessorIds == null || responsibleProfessorIds.size() != 1
                || responsibleProfessorIds.get(0) == null || !professorIds.contains(responsibleProfessorIds.get(0))) throw new InvalidTurmaException();
        List<Professor> found = professors.findAllById(professorIds);
        if (found.size() != new HashSet<>(professorIds).size() || found.stream().anyMatch(p -> !"ativo".equals(p.getStatus()))) throw new InvalidTurmaException();
        Set<Professor> associated = new java.util.LinkedHashSet<>(found);
        Professor responsible = found.stream().filter(p -> p.getId().equals(responsibleProfessorIds.get(0))).findFirst().orElseThrow(InvalidTurmaException::new);
        return new Selection(associated, responsible);
    }

    private TurmaResult result(Turma turma) {
        return new TurmaResult(turma.getId(), turma.getName(), turma.getWeekday(), turma.getStartTime(), turma.getEndTime(),
                turma.getMaxCapacity(), turma.getStatus(), turma.getProfessors().stream().map(this::professorResult).toList(),
                professorResult(turma.getResponsibleProfessor()));
    }

    private ProfessorResult professorResult(Professor professor) {
        return new ProfessorResult(professor.getId(), professor.getUser().getName(), professor.getCref(), professor.getStatus());
    }
    private void requireAdmin(UserRole role) { if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized"); }

    private record Selection(Set<Professor> professors, Professor responsible) {}
    public record ProfessorResult(Long id, String name, String cref, String status) {}
    public record TurmaResult(Long id, String name, String weekday, LocalTime startTime, LocalTime endTime,
                              Integer maxCapacity, String status, List<ProfessorResult> professors,
                              ProfessorResult responsibleProfessor) {}
    public static class InvalidTurmaException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
}
