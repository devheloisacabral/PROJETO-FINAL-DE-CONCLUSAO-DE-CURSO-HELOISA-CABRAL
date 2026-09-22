package devheloisacabral.tcc.com.esportiva.domain.presenca;

import devheloisacabral.tcc.com.esportiva.domain.aula.Aula;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendances", uniqueConstraints = @UniqueConstraint(name = "uk_attendances_student_lesson", columnNames = {"student_id", "lesson_id"}))
public class Presenca {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AuthUser student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Aula aula;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    protected Presenca() {}

    public Presenca(AuthUser student, Aula aula, LocalDateTime recordedAt) {
        this.student = student;
        this.aula = aula;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public AuthUser getStudent() { return student; }
    public Aula getAula() { return aula; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
}
