package devheloisacabral.tcc.com.esportiva.persistence.auth;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByEmail(String email);

    List<AuthUser> findAllByRole(UserRole role);

    long countByRole(UserRole role);

    Optional<AuthUser> findByIdAndRole(Long id, UserRole role);
}
