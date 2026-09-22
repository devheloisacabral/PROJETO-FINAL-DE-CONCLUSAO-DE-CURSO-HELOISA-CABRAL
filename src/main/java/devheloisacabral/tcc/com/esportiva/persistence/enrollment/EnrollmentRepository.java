package devheloisacabral.tcc.com.esportiva.persistence.enrollment;

import devheloisacabral.tcc.com.esportiva.domain.enrollment.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    long countByTurmaId(Long turmaId);

    @Query("select e from Enrollment e join fetch e.student join fetch e.turma order by e.id")
    List<Enrollment> findAllWithStudentAndTurma();

    @Query("select e from Enrollment e join fetch e.student join fetch e.turma where e.student.id = :studentId order by e.id")
    List<Enrollment> findAllWithStudentAndTurmaByStudentId(@Param("studentId") Long studentId);
}
