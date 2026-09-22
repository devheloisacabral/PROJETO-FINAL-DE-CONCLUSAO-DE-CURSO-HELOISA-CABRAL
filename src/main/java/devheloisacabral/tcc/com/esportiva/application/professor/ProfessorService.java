package devheloisacabral.tcc.com.esportiva.application.professor;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.professor.Professor;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.professor.ProfessorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class ProfessorService {
    private final ProfessorRepository professors;
    private final AuthUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public ProfessorService(ProfessorRepository professors, AuthUserRepository users, PasswordEncoder passwordEncoder) {
        this.professors = professors;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ProfessorResult create(String name, String email, LocalDate birthDate, String cref, String password, UserRole role) {
        requireAdmin(role);
        String normalizedEmail = normalizeEmail(email);
        if (users.findByEmail(normalizedEmail).isPresent()) throw new EmailAlreadyUsedException();
        try {
            AuthUser user = users.save(new AuthUser(name.trim(), normalizedEmail, birthDate,
                    passwordEncoder.encode(password), UserRole.PROFESSOR));
            return result(professors.save(new Professor(user, cref.trim())));
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyUsedException();
        }
    }

    @Transactional(readOnly = true)
    public ProfessorResult findOne(Long id, UserRole role) {
        requireAdmin(role);
        return result(professors.findById(id).orElseThrow(NotFoundException::new));
    }

    @Transactional(readOnly = true)
    public List<ProfessorResult> findAll(UserRole role) {
        requireAdmin(role);
        return professors.findAllByOrderByIdAsc().stream().map(this::result).toList();
    }

    @Transactional
    public ProfessorResult update(Long id, String name, String email, LocalDate birthDate, String cref, UserRole role) {
        requireAdmin(role);
        Professor professor = professors.findById(id).orElseThrow(NotFoundException::new);
        String normalizedEmail = normalizeEmail(email);
        users.findByEmail(normalizedEmail).filter(user -> user != professor.getUser()
                        && !(user.getId() != null && Objects.equals(user.getId(), professor.getUser().getId())))
                .ifPresent(user -> { throw new EmailAlreadyUsedException(); });
        professor.update(name.trim(), normalizedEmail, birthDate, cref.trim());
        try {
            return result(professors.save(professor));
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyUsedException();
        }
    }

    @Transactional
    public void inactivate(Long id, UserRole role) { changeStatus(id, role, false); }

    @Transactional
    public void reactivate(Long id, UserRole role) { changeStatus(id, role, true); }

    @Transactional
    public void delete(Long id, UserRole role) {
        requireAdmin(role);
        Professor professor = professors.findById(id).orElseThrow(NotFoundException::new);
        professors.delete(professor);
    }

    private void changeStatus(Long id, UserRole role, boolean active) {
        requireAdmin(role);
        Professor professor = professors.findById(id).orElseThrow(NotFoundException::new);
        if (active) professor.reactivate(); else professor.inactivate();
    }

    private ProfessorResult result(Professor professor) {
        AuthUser user = professor.getUser();
        return new ProfessorResult(professor.getId(), user.getName(), user.getEmail(), user.getBirthDate(), professor.getCref(), professor.getStatus());
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(); }
    private void requireAdmin(UserRole role) { if (role != UserRole.ADMIN) throw new AccessDeniedException("administrative operation is not authorized"); }

    public record ProfessorResult(Long id, String name, String email, LocalDate birthDate, String cref, String status) {}
    public static class EmailAlreadyUsedException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
}
