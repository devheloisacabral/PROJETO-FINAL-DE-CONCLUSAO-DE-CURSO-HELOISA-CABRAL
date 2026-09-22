package devheloisacabral.tcc.com.esportiva.domain.monthlyfee;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "monthly_fee_payments")
public class MonthlyFeePayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "monthly_fee_id", nullable = false)
    private MonthlyFee monthlyFee;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 20)
    private String status;

    protected MonthlyFeePayment() {}
    public MonthlyFeePayment(MonthlyFee monthlyFee, BigDecimal amount) {
        this.monthlyFee = monthlyFee;
        this.amount = amount;
        this.status = "valido";
    }
    public Long getId() { return id; }
    public MonthlyFee getMonthlyFee() { return monthlyFee; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public void cancel() { status = "cancelado"; }
}
