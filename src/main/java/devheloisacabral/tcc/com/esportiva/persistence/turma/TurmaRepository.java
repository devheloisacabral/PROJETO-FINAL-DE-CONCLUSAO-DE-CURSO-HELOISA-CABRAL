package devheloisacabral.tcc.com.esportiva.persistence.turma;

import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
    List<Turma> findAllByOrderByIdAsc();
    List<Turma> findAllByProfessors_IdOrderByIdAsc(Long professorId);
    boolean existsByIdAndProfessors_Id(Long id, Long professorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Turma t where t.id = :id")
    java.util.Optional<Turma> findByIdForEnrollment(@Param("id") Long id);
}
