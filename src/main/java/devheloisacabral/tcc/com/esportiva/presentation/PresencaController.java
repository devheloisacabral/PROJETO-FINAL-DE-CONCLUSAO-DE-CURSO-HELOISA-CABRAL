package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.presenca.PresencaService;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/presencas")
public class PresencaController {
    private final PresencaService service;

    public PresencaController(PresencaService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresencaService.PresencaResult create(@Valid @RequestBody PresencaRequest request, Authentication authentication) {
        return service.create(request.alunoId(), request.aulaId(), role(authentication), userId(authentication));
    }

    @GetMapping
    public List<PresencaService.PresencaResult> findAll(@RequestParam(required = false) Long alunoId,
                                                         Authentication authentication) {
        return service.findAll(role(authentication), userId(authentication), alunoId);
    }

    private UserRole role(Authentication authentication) { return UserRole.valueOf(authentication.getAuthorities().iterator().next().getAuthority().substring(5)); }
    private Long userId(Authentication authentication) { return Long.valueOf(authentication.getName()); }

    public record PresencaRequest(@NotNull Long alunoId, @NotNull Long aulaId) {}
}
