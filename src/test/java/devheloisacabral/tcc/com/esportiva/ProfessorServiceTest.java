package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.professor.ProfessorService;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.professor.Professor;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.professor.ProfessorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ProfessorServiceTest {
    @Mock ProfessorRepository professors;
    @Mock AuthUserRepository users;
    @Mock PasswordEncoder passwordEncoder;
    private ProfessorService service;
    private AuthUser user;
    private Professor professor;

    @BeforeEach
    void setUp() {
        service = new ProfessorService(professors, users, passwordEncoder);
        user = new AuthUser("Maria", "maria@example.com", LocalDate.of(1990, 1, 2), "encoded", UserRole.PROFESSOR);
        professor = new Professor(user, "CREF-123");
    }

    @Test
    void createsActiveProfessorWithProtectedPasswordAndProfessorRole() {
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("bcrypt-hash");
        when(users.save(any(AuthUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(professors.save(any(Professor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(" Maria ", "MARIA@EXAMPLE.COM", LocalDate.of(1990, 1, 2), " CREF-123 ", "secret", UserRole.ADMIN);

        ArgumentCaptor<AuthUser> savedUser = ArgumentCaptor.forClass(AuthUser.class);
        verify(users).save(savedUser.capture());
        assertEquals(UserRole.PROFESSOR, savedUser.getValue().getRole());
        assertEquals("ativo", result.status());
        assertEquals("bcrypt-hash", savedUser.getValue().getPasswordHash());
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void rejectsCreationByNonAdminAndDuplicateEmail() {
        assertThrows(AccessDeniedException.class, () -> service.create("Maria", "maria@example.com", LocalDate.now().minusYears(30), "CREF-123", "secret", UserRole.PROFESSOR));
        when(users.findByEmail("maria@example.com")).thenReturn(Optional.of(user));
        assertThrows(ProfessorService.EmailAlreadyUsedException.class, () -> service.create("Maria", "maria@example.com", LocalDate.now().minusYears(30), "CREF-123", "secret", UserRole.ADMIN));
        verify(users, never()).save(any());
    }

    @Test
    void listsAndFindsProfessorWithoutCredentials() {
        when(professors.findById(1L)).thenReturn(Optional.of(professor));
        when(professors.findAllByOrderByIdAsc()).thenReturn(List.of(professor));

        assertEquals("maria@example.com", service.findOne(1L, UserRole.ADMIN).email());
        assertEquals(1, service.findAll(UserRole.ADMIN).size());
        assertFalse(service.findOne(1L, UserRole.ADMIN).toString().contains("encoded"));
    }

    @Test
    void updatesRegistrationAndPreservesRoleAndPassword() {
        when(professors.findById(1L)).thenReturn(Optional.of(professor));
        when(users.findByEmail("nova@example.com")).thenReturn(Optional.empty());
        when(professors.save(professor)).thenReturn(professor);

        var result = service.update(1L, "Nova Maria", "NOVA@EXAMPLE.COM", LocalDate.of(1991, 2, 3), "CREF-999", UserRole.ADMIN);

        assertEquals("Nova Maria", result.name());
        assertEquals("nova@example.com", result.email());
        assertEquals("CREF-999", result.cref());
        assertEquals(UserRole.PROFESSOR, user.getRole());
        assertEquals("encoded", user.getPasswordHash());
    }

    @Test
    void rejectsUpdateWithAnotherEmailAndNonAdmin() {
        when(professors.findById(1L)).thenReturn(Optional.of(professor));
        when(users.findByEmail("used@example.com")).thenReturn(Optional.of(new AuthUser("used@example.com", "hash", UserRole.ALUNO)));
        assertThrows(ProfessorService.EmailAlreadyUsedException.class, () -> service.update(1L, "Nova", "used@example.com", LocalDate.now().minusYears(30), "CREF-9", UserRole.ADMIN));
        assertThrows(AccessDeniedException.class, () -> service.update(1L, "Nova", "nova@example.com", LocalDate.now().minusYears(30), "CREF-9", UserRole.ALUNO));
        assertEquals("Maria", user.getName());
    }

    @Test
    void changesStatusAndDeletesProfessorOnlyForAdmin() {
        when(professors.findById(1L)).thenReturn(Optional.of(professor));
        service.inactivate(1L, UserRole.ADMIN);
        assertEquals("inativo", professor.getStatus());
        service.reactivate(1L, UserRole.ADMIN);
        assertEquals("ativo", professor.getStatus());
        assertThrows(AccessDeniedException.class, () -> service.inactivate(1L, UserRole.ALUNO));
        service.delete(1L, UserRole.ADMIN);
        verify(professors).delete(professor);
    }

    @Test
    void rejectsAdministrativeOperationsForNonAdmin() {
        assertThrows(AccessDeniedException.class, () -> service.findAll(UserRole.ALUNO));
        assertThrows(AccessDeniedException.class, () -> service.findOne(1L, UserRole.PROFESSOR));
        assertThrows(AccessDeniedException.class, () -> service.delete(1L, UserRole.PROFESSOR));
    }
}
