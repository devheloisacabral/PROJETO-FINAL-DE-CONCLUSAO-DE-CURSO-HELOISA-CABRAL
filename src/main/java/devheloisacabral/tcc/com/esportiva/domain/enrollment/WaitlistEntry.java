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
@Table(name = "class_waitlist")
public class WaitlistEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AuthUser student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private Turma turma;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(nullable = false, length = 20)
    private String status;

    protected WaitlistEntry() {}

    public WaitlistEntry(AuthUser student, Turma turma, LocalDateTime requestedAt) {
        this.student = student;
        this.turma = turma;
        this.requestedAt = requestedAt;
        this.status = "ativa";
    }

    public Long getId() { return id; }
    public AuthUser getStudent() { return student; }
    public Turma getTurma() { return turma; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public String getStatus() { return status; }
    public void promote() { status = "promovida"; }
}
