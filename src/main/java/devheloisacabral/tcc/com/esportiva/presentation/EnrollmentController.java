package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.enrollment.EnrollmentService;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/matriculas")
public class EnrollmentController {
    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentService.EnrollmentResult create(@Valid @RequestBody EnrollmentRequest request,
                                                     Authentication authentication) {
        return service.create(request.studentId(), request.turmaId(), role(authentication));
    }

    @GetMapping
    public List<EnrollmentService.EnrollmentResult> findAll(Authentication authentication) {
        return service.findAll(role(authentication), Long.valueOf(authentication.getName()));
    }

    @PatchMapping("/{id}/cancelar")
    public EnrollmentService.EnrollmentResult cancel(@PathVariable Long id, Authentication authentication) {
        return service.cancel(id, role(authentication));
    }

    @PatchMapping("/{id}/transferir")
    public EnrollmentService.TransferResult transfer(@PathVariable Long id,
                                                     @Valid @RequestBody TransferRequest request,
                                                     Authentication authentication) {
        return service.transfer(id, request.turmaId(), role(authentication));
    }

    private UserRole role(Authentication authentication) {
        return UserRole.valueOf(authentication.getAuthorities().iterator().next().getAuthority().substring(5));
    }

    public record EnrollmentRequest(@NotNull Long studentId, @NotNull Long turmaId) {}
    public record TransferRequest(@NotNull Long turmaId) {}
}
