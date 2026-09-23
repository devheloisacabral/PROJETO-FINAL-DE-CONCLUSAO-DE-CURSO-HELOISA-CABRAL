package devheloisacabral.tcc.com.esportiva.domain.monthlyfee;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "monthly_fee_refunds")
public class MonthlyFeeRefund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "payment_id", nullable = false)
    private MonthlyFeePayment payment;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected MonthlyFeeRefund() {}
    public MonthlyFeeRefund(MonthlyFeePayment payment) {
        this.payment = payment;
        this.amount = payment.getAmount();
    }
    public Long getId() { return id; }
    public MonthlyFeePayment getPayment() { return payment; }
    public BigDecimal getAmount() { return amount; }
}
