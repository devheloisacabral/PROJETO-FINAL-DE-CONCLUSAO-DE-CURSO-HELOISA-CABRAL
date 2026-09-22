package devheloisacabral.tcc.com.esportiva.domain.aula;

import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "lessons")
public class Aula {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private Turma turma;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private boolean cancelled;

    protected Aula() {}

    public Aula(Turma turma, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.turma = turma;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.cancelled = false;
    }

    public Long getId() { return id; }
    public Turma getTurma() { return turma; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public boolean isCancelled() { return cancelled; }
}
