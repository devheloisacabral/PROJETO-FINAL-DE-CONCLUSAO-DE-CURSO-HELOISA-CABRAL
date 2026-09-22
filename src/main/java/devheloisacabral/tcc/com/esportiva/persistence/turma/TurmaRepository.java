package devheloisacabral.tcc.com.esportiva.persistence.turma;

import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
    List<Turma> findAllByOrderByIdAsc();
    List<Turma> findAllByProfessors_IdOrderByIdAsc(Long professorId);
    boolean existsByIdAndProfessors_Id(Long id, Long professorId);
}
