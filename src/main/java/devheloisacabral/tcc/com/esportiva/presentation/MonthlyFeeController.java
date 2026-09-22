package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.monthlyfee.MonthlyFeeService;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mensalidades")
public class MonthlyFeeController {
    private final MonthlyFeeService service;
    public MonthlyFeeController(MonthlyFeeService service) { this.service = service; }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonthlyFeeService.FeeResult create(@Valid @RequestBody CreateFeeRequest request, Authentication a) { return service.create(request.studentId(), request.referenceMonth().atDay(1), role(a)); }
    @PatchMapping("/{id}/receber")
    public MonthlyFeeService.FeeResult receive(@PathVariable Long id, @Valid @RequestBody(required = false) ReceivePaymentRequest request, Authentication a) { return request == null ? service.receive(id, role(a)) : service.receive(id, request.amount(), role(a)); }
    @PatchMapping("/pagamentos/{paymentId}/cancelar")
    public MonthlyFeeService.FeeResult cancelPayment(@PathVariable Long paymentId, Authentication a) { return service.cancelPayment(paymentId, role(a)); }
    @PatchMapping("/{id}/cancelar")
    public MonthlyFeeService.FeeResult cancel(@PathVariable Long id, Authentication a) { return service.cancel(id, role(a)); }
    @GetMapping
    public List<MonthlyFeeService.FeeResult> find(Authentication a) { return service.find(userId(a), role(a)); }
    @GetMapping("/{id}")
    public MonthlyFeeService.FeeResult findOne(@PathVariable Long id, Authentication a) { return service.findOne(id, userId(a), role(a)); }
    private UserRole role(Authentication a) { return UserRole.valueOf(a.getAuthorities().iterator().next().getAuthority().substring(5)); }
    private Long userId(Authentication a) { return Long.valueOf(a.getName()); }
    public record CreateFeeRequest(@NotNull Long studentId, @NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth referenceMonth) {}
    public record ReceivePaymentRequest(@NotNull @jakarta.validation.constraints.DecimalMin("0.01") BigDecimal amount) {}
}
