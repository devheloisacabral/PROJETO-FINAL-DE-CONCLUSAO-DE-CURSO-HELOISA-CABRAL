package devheloisacabral.tcc.com.esportiva.application.dashboard;

import devheloisacabral.tcc.com.esportiva.domain.aula.Aula;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.persistence.aula.AulaRepository;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.enrollment.EnrollmentRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.professor.ProfessorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;

@Service
public class DashboardService {
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM")
            .withResolverStyle(ResolverStyle.STRICT);

    private final AuthUserRepository users;
    private final ProfessorRepository professors;
    private final EnrollmentRepository enrollments;
    private final MonthlyFeeRepository monthlyFees;
    private final AulaRepository aulas;

    public DashboardService(AuthUserRepository users, ProfessorRepository professors,
                            EnrollmentRepository enrollments, MonthlyFeeRepository monthlyFees,
                            AulaRepository aulas) {
        this.users = users;
        this.professors = professors;
        this.enrollments = enrollments;
        this.monthlyFees = monthlyFees;
        this.aulas = aulas;
    }

    @Transactional(readOnly = true)
    public DashboardResult find(String month, UserRole role) {
        if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized");
        YearMonth referenceMonth = parseMonth(month);
        List<Aula> upcoming = aulas.findUpcomingWithTurma(LocalDate.now(), LocalTime.now(), PageRequest.of(0, 5));
        return new DashboardResult(referenceMonth.toString(), users.countByRole(UserRole.ALUNO), professors.count(),
                enrollments.countActiveEnrollments(), monthlyFees.countByReferenceMonthAndStatus(referenceMonth.atDay(1), "pendente"),
                upcoming.stream().map(this::lesson).toList());
    }

    private YearMonth parseMonth(String month) {
        if (month == null) throw new InvalidMonthException();
        try {
            return YearMonth.parse(month, MONTH_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new InvalidMonthException();
        }
    }

    private UpcomingLesson lesson(Aula aula) {
        return new UpcomingLesson(aula.getId(), aula.getDate(), aula.getStartTime(), aula.getEndTime(),
                aula.getTurma().getId(), aula.getTurma().getName(), aula.isCancelled());
    }

    public record DashboardResult(String mesReferencia, long totalAlunos, long totalProfessores,
                                  long matriculasAtivas, long mensalidadesPendentes,
                                  List<UpcomingLesson> proximasAulas) {}

    public record UpcomingLesson(Long id, LocalDate data, LocalTime horarioInicio, LocalTime horarioFim,
                                 Long turmaId, String turmaNome, boolean cancelada) {}

    public static class InvalidMonthException extends RuntimeException {}
}
