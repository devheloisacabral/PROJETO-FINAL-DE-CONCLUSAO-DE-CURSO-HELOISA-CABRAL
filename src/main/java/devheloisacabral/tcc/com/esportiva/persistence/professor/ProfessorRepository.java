package devheloisacabral.tcc.com.esportiva.persistence.professor;

import devheloisacabral.tcc.com.esportiva.domain.professor.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfessorRepository extends JpaRepository<Professor, Long> {
    List<Professor> findAllByOrderByIdAsc();
    java.util.Optional<Professor> findByUser_Id(Long userId);
}
