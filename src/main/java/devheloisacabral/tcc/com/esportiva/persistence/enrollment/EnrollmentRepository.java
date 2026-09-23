package devheloisacabral.tcc.com.esportiva.persistence.enrollment;

import devheloisacabral.tcc.com.esportiva.domain.enrollment.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    long countByTurmaIdAndStatus(Long turmaId, String status);

    boolean existsByStudentIdAndTurmaIdAndStatus(Long studentId, Long turmaId, String status);

    @Query("select count(e) from Enrollment e where e.status = 'ativa' and e.student.role = devheloisacabral.tcc.com.esportiva.domain.auth.UserRole.ALUNO and e.student.status = 'ativo' and e.turma.status = 'ativa'")
    long countActiveEnrollments();

    @Query("select e from Enrollment e join fetch e.student join fetch e.turma order by e.id")
    List<Enrollment> findAllWithStudentAndTurma();

    @Query("select e from Enrollment e join fetch e.student join fetch e.turma where e.student.id = :studentId order by e.id")
    List<Enrollment> findAllWithStudentAndTurmaByStudentId(@Param("studentId") Long studentId);
}
