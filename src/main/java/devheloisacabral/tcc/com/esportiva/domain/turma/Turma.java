package devheloisacabral.tcc.com.esportiva.domain.turma;

import devheloisacabral.tcc.com.esportiva.domain.professor.Professor;
import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "classes")
public class Turma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 20)
    private String weekday;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(nullable = false, length = 20)
    private String status;

    @ManyToMany
    @JoinTable(name = "class_professors",
            joinColumns = @JoinColumn(name = "class_id"),
            inverseJoinColumns = @JoinColumn(name = "professor_id"))
    private Set<Professor> professors = new LinkedHashSet<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "responsible_professor_id", nullable = false)
    private Professor responsibleProfessor;

    protected Turma() {}

    public Turma(String name, String weekday, LocalTime startTime, LocalTime endTime,
                 Integer maxCapacity, String status, Set<Professor> professors,
                 Professor responsibleProfessor) {
        update(name, weekday, startTime, endTime, maxCapacity, status, professors, responsibleProfessor);
    }

    public void update(String name, String weekday, LocalTime startTime, LocalTime endTime,
                       Integer maxCapacity, String status, Set<Professor> professors,
                       Professor responsibleProfessor) {
        this.name = name;
        this.weekday = weekday;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxCapacity = maxCapacity;
        this.status = status;
        this.professors = new LinkedHashSet<>(professors);
        this.responsibleProfessor = responsibleProfessor;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getWeekday() { return weekday; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Integer getMaxCapacity() { return maxCapacity; }
    public String getStatus() { return status; }
    public Set<Professor> getProfessors() { return professors; }
    public Professor getResponsibleProfessor() { return responsibleProfessor; }
    public void inactivate() { status = "inativa"; }
    public void reactivate() { status = "ativa"; }
}
