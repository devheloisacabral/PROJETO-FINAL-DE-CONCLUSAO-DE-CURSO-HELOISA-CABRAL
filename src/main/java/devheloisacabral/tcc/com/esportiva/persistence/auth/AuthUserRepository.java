package devheloisacabral.tcc.com.esportiva.persistence.auth;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByEmail(String email);
}
