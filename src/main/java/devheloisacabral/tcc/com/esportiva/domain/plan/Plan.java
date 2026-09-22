package devheloisacabral.tcc.com.esportiva.domain.plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "plans")
public class Plan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    protected Plan() {}
    public Plan(String name, BigDecimal amount) { this.name = name; this.amount = amount; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
}
