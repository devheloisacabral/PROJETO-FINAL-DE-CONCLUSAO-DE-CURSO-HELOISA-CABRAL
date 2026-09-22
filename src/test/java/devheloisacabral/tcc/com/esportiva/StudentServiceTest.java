package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.student.StudentService;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock AuthUserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks StudentService service;

    @Test
    void shouldCreateInactiveStudentWithProtectedPasswordAndNoPasswordInResponse() {
        when(users.findByEmail("aluno@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senha-segura")).thenReturn("{bcrypt}protected");
        when(users.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(" Ana Silva ", "ALUNO@example.com", "11999999999",
                LocalDate.of(2000, 1, 2), "senha-segura");

        var captor = ArgumentCaptor.forClass(AuthUser.class);
        verify(users).save(captor.capture());
        AuthUser saved = captor.getValue();
        assertEquals(UserRole.ALUNO, saved.getRole());
        assertEquals("inativo", saved.getStatus());
        assertEquals("{bcrypt}protected", saved.getPasswordHash());
        assertNotEquals("senha-segura", saved.getPasswordHash());
        assertEquals("Ana Silva", result.name());
        assertFalse(result.toString().contains("senha-segura"));
    }

    @Test
    void shouldRejectAnAlreadyUsedEmailWithoutCreatingAnotherStudent() {
        when(users.findByEmail("used@example.com")).thenReturn(Optional.of(new AuthUser("used@example.com", "hash", UserRole.ALUNO)));

        assertThrows(StudentService.EmailAlreadyUsedException.class, () -> service.create(
                "Aluno", "used@example.com", "11999999999", LocalDate.of(2000, 1, 2), "senha-segura"));
        verify(users, never()).save(any());
    }

    @Test
    void shouldReturnAllStudentsToAdminAndOnlyTheOwnStudentToStudent() {
        AuthUser first = new AuthUser("A", "a@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        AuthUser second = new AuthUser("B", "b@example.com", "2", LocalDate.of(2000, 1, 1), "hash");
        when(users.findAllByRole(UserRole.ALUNO)).thenReturn(List.of(first, second));
        when(users.findByIdAndRole(7L, UserRole.ALUNO)).thenReturn(Optional.of(first));

        assertEquals(2, service.findFor(1L, UserRole.ADMIN).size());
        assertEquals(1, service.findFor(7L, UserRole.ALUNO).size());
        verify(users).findByIdAndRole(7L, UserRole.ALUNO);
    }

    @Test
    void shouldNotAllowProfessorIndiscriminateStudentQuery() {
        assertThrows(AccessDeniedException.class, () -> service.findFor(4L, UserRole.PROFESSOR));
        verifyNoInteractions(users);
    }

    @Test
    void shouldConfigureAValidIndividualDueDayForAnAdmin() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        when(users.findByIdAndRole(7L, UserRole.ALUNO)).thenReturn(Optional.of(student));

        var result = service.configureDueDay(7L, 15, UserRole.ADMIN);

        assertEquals(15, result.dueDay());
        assertEquals(15, student.getDueDay());
    }

    @Test
    void shouldRejectAnInvalidIndividualDueDayWithoutChangingTheStudent() {
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        when(users.findByIdAndRole(7L, UserRole.ALUNO)).thenReturn(Optional.of(student));

        assertThrows(AuthUser.InvalidDueDayException.class,
                () -> service.configureDueDay(7L, 31, UserRole.ADMIN));
        assertEquals(10, student.getDueDay());
    }
}
