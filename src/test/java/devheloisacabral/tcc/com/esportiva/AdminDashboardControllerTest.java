package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.application.dashboard.DashboardService;
import devheloisacabral.tcc.com.esportiva.presentation.AdminDashboardController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {
    @Mock DashboardService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(service))
                .setControllerAdvice(new devheloisacabral.tcc.com.esportiva.presentation.AuthenticationExceptionHandler())
                .build();
    }

    @Test
    void returnsTheDashboardContractForAnAdmin() throws Exception {
        when(service.find(eq("2026-09"), eq(devheloisacabral.tcc.com.esportiva.domain.auth.UserRole.ADMIN)))
                .thenReturn(new DashboardService.DashboardResult("2026-09", 7, 3, 5, 4,
                        List.of(new DashboardService.UpcomingLesson(80L, LocalDate.of(2026, 9, 25),
                                LocalTime.of(18, 0), LocalTime.of(19, 0), 12L, "Turma Adulto", false))));

        mockMvc.perform(get("/admin/dashboard?mes=2026-09").principal(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesReferencia").value("2026-09"))
                .andExpect(jsonPath("$.totalAlunos").value(7))
                .andExpect(jsonPath("$.totalProfessores").value(3))
                .andExpect(jsonPath("$.matriculasAtivas").value(5))
                .andExpect(jsonPath("$.mensalidadesPendentes").value(4))
                .andExpect(jsonPath("$.proximasAulas[0].turmaId").value(12))
                .andExpect(jsonPath("$.proximasAulas[0].turmaNome").value("Turma Adulto"));
    }

    private UsernamePasswordAuthenticationToken admin() {
        return new UsernamePasswordAuthenticationToken("1", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}
