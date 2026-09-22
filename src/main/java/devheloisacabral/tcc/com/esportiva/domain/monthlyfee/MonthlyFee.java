package devheloisacabral.tcc.com.esportiva.domain.monthlyfee;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "monthly_fees", uniqueConstraints = @UniqueConstraint(name = "uk_monthly_fees_student_month", columnNames = {"student_id", "reference_month"}))
public class MonthlyFee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "student_id", nullable = false)
    private AuthUser student;
    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 20)
    private String status;

    protected MonthlyFee() {}
    public MonthlyFee(AuthUser student, LocalDate referenceMonth, BigDecimal amount) {
        this.student = student; this.referenceMonth = referenceMonth; this.dueDate = referenceMonth.withDayOfMonth(10); this.amount = amount; this.status = "pendente";
    }
    public Long getId() { return id; }
    public AuthUser getStudent() { return student; }
    public LocalDate getReferenceMonth() { return referenceMonth; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public void pay() { status = "paga"; }
    public void cancelPayment() { status = "pendente"; }
}
