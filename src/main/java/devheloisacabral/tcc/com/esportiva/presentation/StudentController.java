package devheloisacabral.tcc.com.esportiva.presentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import devheloisacabral.tcc.com.esportiva.application.student.StudentService;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/alunos")
public class StudentController {
    private final StudentService service;
    private final devheloisacabral.tcc.com.esportiva.application.plan.PlanService planService;

    public StudentController(StudentService service, devheloisacabral.tcc.com.esportiva.application.plan.PlanService planService) { this.service = service; this.planService = planService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentService.StudentResult create(@Valid @RequestBody CreateStudentRequest request) {
        return service.create(request.name(), request.email(), request.phone(), request.birthDate(), request.password());
    }

    @GetMapping
    public List<StudentService.StudentResult> find(Authentication authentication) {
        UserRole role = UserRole.valueOf(authentication.getAuthorities().iterator().next().getAuthority().substring(5));
        return service.findFor(Long.parseLong(authentication.getName()), role);
    }

    @PutMapping("/{studentId}/plano")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void associatePlan(@PathVariable Long studentId, @Valid @RequestBody AssociatePlanRequest request, Authentication authentication) {
        UserRole role = UserRole.valueOf(authentication.getAuthorities().iterator().next().getAuthority().substring(5));
        planService.associate(studentId, request.planId(), role);
    }

    @PatchMapping("/{studentId}/vencimento")
    public StudentService.StudentResult configureDueDay(@PathVariable Long studentId, @Valid @RequestBody ConfigureDueDayRequest request, Authentication authentication) {
        UserRole role = UserRole.valueOf(authentication.getAuthorities().iterator().next().getAuthority().substring(5));
        return service.configureDueDay(studentId, request.dueDay(), role);
    }

    @JsonIgnoreProperties(ignoreUnknown = false)
    public record CreateStudentRequest(@NotBlank String name, @NotBlank @Email String email,
                                       @NotBlank String phone, @NotNull @Past LocalDate birthDate,
                                       @NotBlank String password) {}
    public record AssociatePlanRequest(@NotNull Long planId) {}
    public record ConfigureDueDayRequest(@NotNull @Min(1) @Max(28) Integer dueDay) {}
}
