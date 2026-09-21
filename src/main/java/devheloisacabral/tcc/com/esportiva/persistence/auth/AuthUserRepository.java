package devheloisacabral.tcc.com.esportiva.persistence.auth;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
}
