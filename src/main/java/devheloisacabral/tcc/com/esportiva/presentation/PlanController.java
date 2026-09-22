package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.plan.PlanService;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/planos")
public class PlanController {
    private final PlanService service;
    public PlanController(PlanService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanService.PlanResult create(@Valid @RequestBody CreatePlanRequest request, Authentication authentication) {
        return service.create(request.name(), request.amount(), role(authentication));
    }
    private UserRole role(Authentication a) { return UserRole.valueOf(a.getAuthorities().iterator().next().getAuthority().substring(5)); }
    public record CreatePlanRequest(@NotBlank String name, @NotNull @DecimalMin("0.01") BigDecimal amount) {}
}
