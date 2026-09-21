package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.auth.AuthenticationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationService service;

    public AuthController(AuthenticationService service) { this.service = service; }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        var tokens = service.login(request.email(), request.password());
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken());
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        var tokens = service.refresh(request.refreshToken());
        return new TokenResponse(tokens.accessToken(), tokens.refreshToken());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        service.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        var claims = (io.jsonwebtoken.Claims) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) authentication).getDetails();
        return Map.of("id", Long.valueOf(authentication.getName()), "email", claims.get("email", String.class), "role", claims.get("role", String.class));
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record TokenResponse(String accessToken, String refreshToken) {}
}
