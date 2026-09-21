package devheloisacabral.tcc.com.esportiva;

import com.fasterxml.jackson.databind.ObjectMapper;
import devheloisacabral.tcc.com.esportiva.presentation.StudentController.CreateStudentRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StudentRequestContractTest {
    @Test
    void shouldRejectStatusSentByClient() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        assertThrows(Exception.class, () -> mapper.readValue("""
                {"name":"Ana","email":"ana@example.com","phone":"11999999999",
                 "birthDate":"2000-01-02","password":"senha-segura","status":"ativo"}
                """, CreateStudentRequest.class));
    }

    @Test
    void shouldRejectMissingBirthDateAsInvalidRequiredData() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var request = new CreateStudentRequest("Ana", "ana@example.com", "11999999999", null, "senha-segura");

        assertTrue(validator.validate(request).stream().anyMatch(error -> error.getPropertyPath().toString().equals("birthDate")));
    }
}
