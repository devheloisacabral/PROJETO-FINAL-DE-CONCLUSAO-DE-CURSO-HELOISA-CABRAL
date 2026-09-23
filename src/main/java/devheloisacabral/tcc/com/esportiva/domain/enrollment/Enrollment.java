package devheloisacabral.tcc.com.esportiva.domain.enrollment;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AuthUser student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private Turma turma;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Column(nullable = false, length = 20)
    private String status;

    protected Enrollment() {}

    public Enrollment(AuthUser student, Turma turma, LocalDateTime enrolledAt) {
        this.student = student;
        this.turma = turma;
        this.enrolledAt = enrolledAt;
        this.status = "ativa";
    }

    public Long getId() { return id; }
    public AuthUser getStudent() { return student; }
    public Turma getTurma() { return turma; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public String getStatus() { return status; }
    public void cancel() { status = "cancelada"; }
}
