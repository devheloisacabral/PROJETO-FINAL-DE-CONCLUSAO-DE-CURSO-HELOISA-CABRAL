package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationContractTest {

    @Test
    void shouldIssueAndParseRs256AccessTokenWithFifteenMinuteLifetime() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var privateKey = generator.generateKeyPair().getPrivate();
        var encoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        var service = new JwtTokenService("-----BEGIN PRIVATE KEY-----" + encoded + "-----END PRIVATE KEY-----", java.time.Duration.ofMinutes(15));
        var user = new AuthUser("admin@example.com", "$2a$10$hash", UserRole.ADMIN);

        String token = service.createAccessToken(user);
        Claims claims = service.parseAccessToken(token);
        assertEquals("access", claims.get("type", String.class));
        assertEquals("admin", claims.get("role", String.class));
        String header = new String(Base64.getUrlDecoder().decode(token.substring(0, token.indexOf('.'))));
        assertTrue(header.contains("\"alg\":\"RS256\""));
        assertTrue(claims.getExpiration().after(new Date(System.currentTimeMillis() + 14 * 60_000L)));
    }
}
