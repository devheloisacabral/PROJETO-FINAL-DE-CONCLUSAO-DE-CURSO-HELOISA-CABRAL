package devheloisacabral.tcc.com.esportiva.persistence.monthlyfee;

import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyFeeRepository extends JpaRepository<MonthlyFee, Long> {
    List<MonthlyFee> findAllByStudentId(Long studentId);
    Optional<MonthlyFee> findByIdAndStudentId(Long id, Long studentId);
}
