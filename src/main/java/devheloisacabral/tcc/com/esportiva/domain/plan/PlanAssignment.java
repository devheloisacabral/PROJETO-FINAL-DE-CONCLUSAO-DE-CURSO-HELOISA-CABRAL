package devheloisacabral.tcc.com.esportiva.domain.plan;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(name = "student_plan_history", uniqueConstraints = @UniqueConstraint(name = "uk_student_plan_history_student_start", columnNames = {"student_id", "valid_from"}))
public class PlanAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private AuthUser student;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    protected PlanAssignment() {}

    public PlanAssignment(AuthUser student, Plan plan, LocalDate validFrom) {
        this.student = student;
        this.plan = plan;
        this.validFrom = validFrom;
    }

    public Long getId() { return id; }
    public AuthUser getStudent() { return student; }
    public Plan getPlan() { return plan; }
    public LocalDate getValidFrom() { return validFrom; }
}
