package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.monthlyfee.MonthlyFee;
import devheloisacabral.tcc.com.esportiva.domain.plan.Plan;
import devheloisacabral.tcc.com.esportiva.persistence.auth.AuthUserRepository;
import devheloisacabral.tcc.com.esportiva.persistence.monthlyfee.MonthlyFeeRepository;
import devheloisacabral.tcc.com.esportiva.persistence.plan.PlanRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
class PlanAndMonthlyFeePersistenceTest {
    @Container static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
    @Autowired PlanRepository plans;
    @Autowired AuthUserRepository users;
    @Autowired MonthlyFeeRepository fees;

    @Test void persistsPlanAssociationAndRejectsDuplicateStudentMonth() {
        Plan plan = plans.save(new Plan("Mensal", new BigDecimal("120.00")));
        AuthUser student = new AuthUser("Ana", "ana@example.com", "1", LocalDate.of(2000, 1, 1), "hash");
        student.setPlan(plan);
        student = users.save(student);
        fees.saveAndFlush(new MonthlyFee(student, LocalDate.of(2026, 9, 1), plan.getAmount()));
        AuthUser persisted = users.findById(student.getId()).orElseThrow();
        assertEquals(plan.getId(), persisted.getPlan().getId());
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> fees.saveAndFlush(new MonthlyFee(persisted, LocalDate.of(2026, 9, 1), plan.getAmount())));
    }
}
