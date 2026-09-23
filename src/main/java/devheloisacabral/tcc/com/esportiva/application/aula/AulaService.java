package devheloisacabral.tcc.com.esportiva.application.aula;

import devheloisacabral.tcc.com.esportiva.domain.aula.Aula;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import devheloisacabral.tcc.com.esportiva.persistence.aula.AulaRepository;
import devheloisacabral.tcc.com.esportiva.persistence.turma.TurmaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
public class AulaService {
    private final AulaRepository aulas;
    private final TurmaRepository turmas;

    public AulaService(AulaRepository aulas, TurmaRepository turmas) {
        this.aulas = aulas;
        this.turmas = turmas;
    }

    @Transactional
    public AulaResult create(Long turmaId, LocalDate date, UserRole role) {
        requireAdmin(role);
        if (turmaId == null || date == null) throw new InvalidAulaException("invalid lesson data");
        Turma turma = turmas.findById(turmaId).orElseThrow(NotFoundException::new);
        if (turma.getStartTime() == null || turma.getEndTime() == null || !turma.getStartTime().isBefore(turma.getEndTime())) {
            throw new InvalidAulaException("invalid lesson times");
        }
        if (!weekdayMatches(turma.getWeekday(), date.getDayOfWeek())) {
            throw new WeekdayMismatchException();
        }
        return result(aulas.save(new Aula(turma, date, turma.getStartTime(), turma.getEndTime())));
    }

    @Transactional(readOnly = true)
    public List<AulaResult> findAll(UserRole role, Long userId) {
        return switch (role) {
            case ADMIN -> aulas.findAllWithTurma().stream().map(this::result).toList();
            case PROFESSOR -> aulas.findAllWithTurmaByProfessorUserId(userId).stream().map(this::result).toList();
            case ALUNO -> aulas.findAllWithTurmaByStudentUserId(userId).stream().map(this::result).toList();
        };
    }

    @Transactional(readOnly = true)
    public AulaResult findOne(Long id, UserRole role, Long userId) {
        Aula aula = aulas.findByIdWithTurma(id).orElseThrow(NotFoundException::new);
        if (role == UserRole.ADMIN) return result(aula);
        if (role == UserRole.PROFESSOR && aula.getTurma().getProfessors().stream()
                .anyMatch(professor -> professor.getUser().getId().equals(userId))) return result(aula);
        if (role == UserRole.ALUNO && aulas.findAllWithTurmaByStudentUserId(userId).stream()
                .anyMatch(candidate -> candidate.getId().equals(id))) return result(aula);
        throw new AccessDeniedException("lesson access is not authorized");
    }

    private boolean weekdayMatches(String configured, DayOfWeek actual) {
        if (configured == null) return false;
        String normalized = configured.trim().toLowerCase(Locale.ROOT);
        String[] weekdays = {"segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado", "domingo"};
        return actual.getValue() <= weekdays.length && weekdays[actual.getValue() - 1].equals(normalized);
    }

    private AulaResult result(Aula aula) {
        Turma turma = aula.getTurma();
        return new AulaResult(aula.getId(), new TurmaResult(turma.getId(), turma.getName()), aula.getDate(),
                aula.getStartTime(), aula.getEndTime(), aula.isCancelled());
    }

    private void requireAdmin(UserRole role) {
        if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized");
    }

    public record AulaResult(Long id, TurmaResult turma, LocalDate data, LocalTime horarioInicio, LocalTime horarioFim, boolean cancelada) {}
    public record TurmaResult(Long id, String name) {}
    public static class InvalidAulaException extends RuntimeException { public InvalidAulaException(String message) { super(message); } }
    public static class WeekdayMismatchException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
}
