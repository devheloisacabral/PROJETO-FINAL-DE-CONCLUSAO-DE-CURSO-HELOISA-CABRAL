package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.auth.AuthenticationService;
import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.RefreshToken;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.auth.RefreshTokenRepository;
import devheloisacabral.tcc.com.esportiva.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProfessorAuthenticationContractTest {
    @Test
    void professorReceivesAccessAndRefreshTokensWithProfessorRole() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String key = Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        var jwt = new JwtTokenService("-----BEGIN PRIVATE KEY-----" + key + "-----END PRIVATE KEY-----", Duration.ofMinutes(15));
        var users = mock(AuthUserRepository.class);
        var refreshTokens = mock(RefreshTokenRepository.class);
        var encoder = mock(PasswordEncoder.class);
        var professor = new AuthUser("Professor", "professor@example.com", null, "bcrypt-hash", UserRole.PROFESSOR);
        when(users.findByEmail("professor@example.com")).thenReturn(Optional.of(professor));
        when(encoder.matches("secret", "bcrypt-hash")).thenReturn(true);
        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var tokens = new AuthenticationService(users, refreshTokens, encoder, jwt, Duration.ofDays(7)).login("professor@example.com", "secret");

        assertTrue(tokens.accessToken().length() > 20);
        assertTrue(tokens.refreshToken().length() > 20);
        assertEquals("professor", jwt.parseAccessToken(tokens.accessToken()).get("role", String.class));
    }
}
