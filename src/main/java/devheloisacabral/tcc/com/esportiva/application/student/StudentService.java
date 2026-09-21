package devheloisacabral.tcc.com.esportiva.application.student;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class StudentService {
    private final AuthUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public StudentService(AuthUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public StudentResult create(String name, String email, String phone, LocalDate birthDate, String password) {
        String normalizedEmail = email.trim().toLowerCase();
        if (users.findByEmail(normalizedEmail).isPresent()) throw new EmailAlreadyUsedException();
        try {
            AuthUser student = users.save(new AuthUser(name.trim(), normalizedEmail, phone.trim(), birthDate,
                    passwordEncoder.encode(password)));
            return StudentResult.from(student);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyUsedException();
        }
    }

    @Transactional(readOnly = true)
    public List<StudentResult> findFor(long authenticatedUserId, UserRole role) {
        if (role == UserRole.ADMIN) return users.findAllByRole(UserRole.ALUNO).stream().map(StudentResult::from).toList();
        if (role == UserRole.ALUNO) return users.findByIdAndRole(authenticatedUserId, UserRole.ALUNO).map(StudentResult::from).stream().toList();
        throw new AccessDeniedException("student access is not authorized");
    }

    public record StudentResult(Long id, String name, String email, String phone, LocalDate birthDate, String status) {
        static StudentResult from(AuthUser user) {
            return new StudentResult(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getBirthDate(), user.getStatus());
        }
    }

    public static class EmailAlreadyUsedException extends RuntimeException {}
}
