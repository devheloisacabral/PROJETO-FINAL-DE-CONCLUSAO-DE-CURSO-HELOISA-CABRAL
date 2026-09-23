package devheloisacabral.tcc.com.esportiva.persistence.turma;

import devheloisacabral.tcc.com.esportiva.domain.turma.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.time.LocalTime;

public interface TurmaRepository extends JpaRepository<Turma, Long> {
    List<Turma> findAllByOrderByIdAsc();
    List<Turma> findAllByProfessors_IdOrderByIdAsc(Long professorId);
    boolean existsByIdAndProfessors_Id(Long id, Long professorId);

    @Query("select case when count(t) > 0 then true else false end from Turma t join t.professors p "
            + "where t.status = 'ativa' and t.weekday = :weekday and p.id in :professorIds "
            + "and t.startTime < :endTime and t.endTime > :startTime "
            + "and (:excludedTurmaId is null or t.id <> :excludedTurmaId)")
    boolean existsActiveScheduleConflict(@Param("weekday") String weekday,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime,
                                         @Param("professorIds") List<Long> professorIds,
                                         @Param("excludedTurmaId") Long excludedTurmaId);

    @Query("select case when count(e1) > 0 then true else false end from Enrollment e1, Enrollment e2 "
            + "where e1.turma.id = :turmaId and e1.status = 'ativa' and e2.status = 'ativa' "
            + "and e2.turma.status = 'ativa' and e2.turma.id <> :turmaId and e2.student = e1.student "
            + "and e2.turma.weekday = :weekday and e2.turma.startTime < :endTime and e2.turma.endTime > :startTime")
    boolean existsActiveEnrollmentScheduleConflict(@Param("turmaId") Long turmaId,
                                                   @Param("weekday") String weekday,
                                                   @Param("startTime") LocalTime startTime,
                                                   @Param("endTime") LocalTime endTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Turma t where t.id = :id")
    java.util.Optional<Turma> findByIdForEnrollment(@Param("id") Long id);
}
