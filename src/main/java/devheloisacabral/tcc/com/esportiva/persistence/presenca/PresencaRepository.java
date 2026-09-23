package devheloisacabral.tcc.com.esportiva.persistence.presenca;

import devheloisacabral.tcc.com.esportiva.domain.presenca.Presenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PresencaRepository extends JpaRepository<Presenca, Long> {
    @Query("select p from Presenca p join fetch p.student join fetch p.aula a join fetch a.turma order by p.id")
    List<Presenca> findAllWithDetails();

    @Query("select distinct p from Presenca p join fetch p.student join fetch p.aula a join fetch a.turma t join t.professors professor where professor.user.id = :userId order by p.id")
    List<Presenca> findAllWithDetailsByProfessorUserId(@Param("userId") Long userId);

    @Query("select p from Presenca p join fetch p.student join fetch p.aula a join fetch a.turma where p.student.id = :studentId order by p.id")
    List<Presenca> findAllWithDetailsByStudentId(@Param("studentId") Long studentId);
}
