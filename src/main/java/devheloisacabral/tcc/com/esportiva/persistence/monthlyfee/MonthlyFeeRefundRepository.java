package devheloisacabral.tcc.com.esportiva.persistence.monthlyfee;

import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFeeRefund;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyFeeRefundRepository extends JpaRepository<MonthlyFeeRefund, Long> {
    List<MonthlyFeeRefund> findAllByPaymentId(Long paymentId);
    List<MonthlyFeeRefund> findAllByPaymentMonthlyFeeId(Long monthlyFeeId);
}
