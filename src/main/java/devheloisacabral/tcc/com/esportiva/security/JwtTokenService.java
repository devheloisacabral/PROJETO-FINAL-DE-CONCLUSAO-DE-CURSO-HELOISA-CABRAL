package devheloisacabral.tcc.com.esportiva.security;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtTokenService {
    private final String privateKeyValue;
    private final Duration accessTtl;

    public JwtTokenService(@Value("${jwt.private-key:}") String privateKeyValue,
                           @Value("${jwt.access-token-ttl:PT15M}") Duration accessTtl) {
        this.privateKeyValue = privateKeyValue;
        this.accessTtl = accessTtl;
    }

    public String createAccessToken(AuthUser user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(user.getId())).claim("email", user.getEmail())
                .claim("role", user.getRole().name().toLowerCase()).claim("type", "access")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(accessTtl)))
                .signWith(privateKey(), Jwts.SIG.RS256).compact();
    }

    public Claims parseAccessToken(String token) {
        Claims claims = Jwts.parser().verifyWith(publicKey()).build().parseSignedClaims(token).getPayload();
        if (!"access".equals(claims.get("type", String.class))) throw new JwtException("invalid token type");
        return claims;
    }

    private RSAPrivateKey privateKey() {
        try {
            String pem = privateKeyValue.replace("\\n", "\n").replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            if (pem.isBlank()) throw new IllegalStateException("JWT private key is not configured");
            PrivateKey key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
            RSAPrivateKey rsa = (RSAPrivateKey) key;
            if (rsa.getModulus().bitLength() < 2048) throw new IllegalStateException("JWT RSA key must have at least 2048 bits");
            return rsa;
        } catch (Exception ex) {
            if (ex instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("Invalid JWT RSA private key configuration", ex);
        }
    }

    private RSAPublicKey publicKey() {
        RSAPrivateKey privateKey = privateKey();
        try {
            if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
                throw new IllegalStateException("JWT RSA private key must contain CRT parameters");
            }
            var spec = new java.security.spec.RSAPublicKeySpec(privateKey.getModulus(), crtKey.getPublicExponent());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) { throw new IllegalStateException("Invalid JWT RSA key configuration", ex); }
    }
}
