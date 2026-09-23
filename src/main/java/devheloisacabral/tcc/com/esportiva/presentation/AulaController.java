package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.aula.AulaService;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/aulas")
public class AulaController {
    private final AulaService service;

    public AulaController(AulaService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AulaService.AulaResult create(@Valid @RequestBody AulaRequest request, Authentication authentication) {
        return service.create(request.turmaId(), request.data(), role(authentication));
    }

    @GetMapping
    public List<AulaService.AulaResult> findAll(Authentication authentication) {
        return service.findAll(role(authentication), userId(authentication));
    }

    @GetMapping("/{id}")
    public AulaService.AulaResult findOne(@PathVariable Long id, Authentication authentication) {
        return service.findOne(id, role(authentication), userId(authentication));
    }

    @PatchMapping("/{id}/reagendar")
    public AulaService.AulaResult reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleRequest request,
                                             Authentication authentication) {
        return service.reschedule(id, request.data(), request.horarioInicio(), request.horarioFim(), role(authentication));
    }

    private UserRole role(Authentication authentication) { return UserRole.valueOf(authentication.getAuthorities().iterator().next().getAuthority().substring(5)); }
    private Long userId(Authentication authentication) { return Long.valueOf(authentication.getName()); }

    public record AulaRequest(@NotNull Long turmaId, @NotNull LocalDate data) {}
    public record RescheduleRequest(@NotNull LocalDate data, @NotNull LocalTime horarioInicio,
                                    @NotNull LocalTime horarioFim) {}
}
