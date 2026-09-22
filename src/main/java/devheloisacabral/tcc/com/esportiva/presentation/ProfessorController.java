package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.professor.ProfessorService;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/professores")
public class ProfessorController {
    private final ProfessorService service;

    public ProfessorController(ProfessorService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessorService.ProfessorResult create(@Valid @RequestBody CreateProfessorRequest request, Authentication authentication) {
        return service.create(request.name(), request.email(), request.birthDate(), request.cref(), request.password(), role(authentication));
    }

    @GetMapping
    public List<ProfessorService.ProfessorResult> findAll(Authentication authentication) { return service.findAll(role(authentication)); }

    @GetMapping("/{id}")
    public ProfessorService.ProfessorResult findOne(@PathVariable Long id, Authentication authentication) { return service.findOne(id, role(authentication)); }

    @PutMapping("/{id}")
    public ProfessorService.ProfessorResult update(@PathVariable Long id, @Valid @RequestBody UpdateProfessorRequest request, Authentication authentication) {
        return service.update(id, request.name(), request.email(), request.birthDate(), request.cref(), role(authentication));
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

    public record CreateProfessorRequest(@NotBlank String name, @NotBlank @Email String email,
                                         @NotNull @Past LocalDate birthDate, @NotBlank String cref, @NotBlank String password) {}
    public record UpdateProfessorRequest(@NotBlank String name, @NotBlank @Email String email,
                                         @NotNull @Past LocalDate birthDate, @NotBlank String cref) {}
}
