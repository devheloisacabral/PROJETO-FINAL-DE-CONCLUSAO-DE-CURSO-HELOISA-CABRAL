package devheloisacabral.tcc.com.esportiva.persistence.user;

import devheloisacabral.tcc.com.esportiva.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
