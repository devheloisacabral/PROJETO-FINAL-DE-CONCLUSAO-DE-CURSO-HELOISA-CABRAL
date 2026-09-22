package devheloisacabral.tcc.com.esportiva.persistence.monthlyfee;

import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFeePayment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyFeePaymentRepository extends JpaRepository<MonthlyFeePayment, Long> {
    List<MonthlyFeePayment> findAllByMonthlyFeeId(Long monthlyFeeId);
}
