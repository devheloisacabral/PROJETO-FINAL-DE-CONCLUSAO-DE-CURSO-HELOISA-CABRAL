package devheloisacabral.tcc.com.esportiva.persistence.enrollment;

import devheloisacabral.tcc.com.esportiva.domain.enrollment.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    boolean existsByStudentIdAndTurmaIdAndStatus(Long studentId, Long turmaId, String status);
    List<WaitlistEntry> findByTurmaIdAndStatusOrderByRequestedAtAscIdAsc(Long turmaId, String status);
}
