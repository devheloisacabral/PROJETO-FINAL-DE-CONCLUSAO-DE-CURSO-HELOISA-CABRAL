package devheloisacabral.tcc.com.esportiva.presentation;

import devheloisacabral.tcc.com.esportiva.application.dashboard.DashboardService;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
public class AdminDashboardController {
    private final DashboardService service;

    public AdminDashboardController(DashboardService service) { this.service = service; }

    @GetMapping
    public DashboardService.DashboardResult find(@RequestParam String mes, Authentication authentication) {
        return service.find(mes, role(authentication));
    }

    private UserRole role(Authentication authentication) {
        return UserRole.valueOf(authentication.getAuthorities().iterator().next().getAuthority().substring(5));
    }
}
