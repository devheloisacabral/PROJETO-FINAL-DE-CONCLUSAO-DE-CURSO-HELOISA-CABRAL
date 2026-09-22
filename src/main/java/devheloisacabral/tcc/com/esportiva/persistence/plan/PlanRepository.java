package devheloisacabral.tcc.com.esportiva.persistence.plan;

import devheloisacabral.tcc.com.esportiva.domain.plan.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {}
