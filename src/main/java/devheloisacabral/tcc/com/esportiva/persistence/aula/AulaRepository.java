package devheloisacabral.tcc.com.esportiva.persistence.aula;

import devheloisacabral.tcc.com.esportiva.domain.aula.Aula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.data.domain.Pageable;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    @Query("select a from Aula a join fetch a.turma order by a.id")
    List<Aula> findAllWithTurma();

    @Query("select distinct a from Aula a join fetch a.turma t join t.professors p where p.user.id = :userId order by a.id")
    List<Aula> findAllWithTurmaByProfessorUserId(@Param("userId") Long userId);

    @Query("select distinct a from Aula a join fetch a.turma t join Enrollment e on e.turma.id = t.id where e.student.id = :userId order by a.id")
    List<Aula> findAllWithTurmaByStudentUserId(@Param("userId") Long userId);

    @Query("select a from Aula a join fetch a.turma where a.id = :id")
    Optional<Aula> findByIdWithTurma(@Param("id") Long id);

    @Query("select a from Aula a join fetch a.turma where a.date > :today or (a.date = :today and a.startTime > :now) order by a.date, a.startTime, a.id")
    List<Aula> findUpcomingWithTurma(@Param("today") LocalDate today, @Param("now") LocalTime now, Pageable pageable);
}
