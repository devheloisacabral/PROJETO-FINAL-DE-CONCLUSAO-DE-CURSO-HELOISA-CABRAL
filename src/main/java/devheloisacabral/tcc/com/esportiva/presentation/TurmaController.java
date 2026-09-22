package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.turma.TurmaService;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/turmas")
public class TurmaController {
    private final TurmaService service;

    public TurmaController(TurmaService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurmaService.TurmaResult create(@Valid @RequestBody TurmaRequest request, Authentication authentication) {
        return service.create(request.name(), request.weekday(), request.startTime(), request.endTime(), request.maxCapacity(),
                request.status(), request.professorIds(), request.responsibleProfessorIds(), role(authentication));
    }

    @GetMapping
    public List<TurmaService.TurmaResult> findAll(Authentication authentication) {
        return service.findAll(role(authentication), userId(authentication));
    }

    @GetMapping("/{id}")
    public TurmaService.TurmaResult findOne(@PathVariable Long id, Authentication authentication) {
        return service.findOne(id, role(authentication), userId(authentication));
    }

    @PutMapping("/{id}")
    public TurmaService.TurmaResult update(@PathVariable Long id, @Valid @RequestBody TurmaRequest request, Authentication authentication) {
        return service.update(id, request.name(), request.weekday(), request.startTime(), request.endTime(), request.maxCapacity(),
                request.status(), request.professorIds(), request.responsibleProfessorIds(), role(authentication));
    }

    @PatchMapping("/{id}/inativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inactivate(@PathVariable Long id, Authentication authentication) { service.inactivate(id, role(authentication)); }

    @PatchMapping("/{id}/reativar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivate(@PathVariable Long id, Authentication authentication) { service.reactivate(id, role(authentication)); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) { service.delete(id, role(authentication)); }

    private UserRole role(Authentication authentication) { return UserRole.valueOf(authentication.getAuthorities().iterator().next().getAuthority().substring(5)); }
    private Long userId(Authentication authentication) { return Long.valueOf(authentication.getName()); }

    public record TurmaRequest(@NotBlank String name, @NotBlank String weekday, @NotNull LocalTime startTime,
                               @NotNull LocalTime endTime, @NotNull Integer maxCapacity, @NotBlank String status,
                               @NotEmpty List<Long> professorIds, @NotEmpty List<Long> responsibleProfessorIds) {}
}
