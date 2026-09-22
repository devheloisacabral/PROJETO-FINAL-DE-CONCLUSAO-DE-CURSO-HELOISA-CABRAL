package devheloisacabral.tcc.com.esportiva.persistence.plan;

import devheloisacabral.tcc.com.esportiva.domain.plan.PlanAssignment;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanAssignmentRepository extends JpaRepository<PlanAssignment, Long> {
    Optional<PlanAssignment> findTopByStudentIdAndValidFromLessThanEqualOrderByValidFromDesc(Long studentId, LocalDate referenceDate);
    boolean existsByStudentIdAndValidFrom(Long studentId, LocalDate validFrom);
}
