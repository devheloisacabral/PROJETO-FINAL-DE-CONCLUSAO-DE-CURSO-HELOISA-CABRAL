package devheloisacabral.tcc.com.esportiva.application.auth;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.RefreshToken;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.auth.RefreshTokenRepository;
import devheloisacabral.tcc.com.esportiva.security.JwtTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthenticationService {
    private final AuthUserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final Duration refreshTtl;
    private final SecureRandom random = new SecureRandom();

    public AuthenticationService(AuthUserRepository users, RefreshTokenRepository refreshTokens,
                                 PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService,
                                 @Value("${jwt.refresh-token-ttl:P7D}") Duration refreshTtl) {
        this.users = users; this.refreshTokens = refreshTokens; this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService; this.refreshTtl = refreshTtl;
    }

    @Transactional
    public TokenPair login(String email, String password) {
        AuthUser user = users.findByEmail(email.trim().toLowerCase()).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) throw new InvalidCredentialsException();
        return issue(user);
    }

    @Transactional
    public TokenPair refresh(String rawToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(hash(rawToken)).orElseThrow(InvalidCredentialsException::new);
        Instant now = Instant.now();
        if (!stored.isUsable(now)) throw new InvalidCredentialsException();
        stored.rotate(now);
        return issue(stored.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(hash(rawToken)).ifPresent(token -> token.revoke(Instant.now()));
    }

    private TokenPair issue(AuthUser user) {
        String rawRefresh = randomToken();
        refreshTokens.save(new RefreshToken(user, hash(rawRefresh), Instant.now().plus(refreshTtl)));
        return new TokenPair(jwtTokenService.createAccessToken(user), rawRefresh);
    }

    private String randomToken() { byte[] value = new byte[32]; random.nextBytes(value); return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    public static String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception ex) { throw new IllegalStateException(ex); } }

    public record TokenPair(String accessToken, String refreshToken) {}
    public static class InvalidCredentialsException extends RuntimeException {}
}
