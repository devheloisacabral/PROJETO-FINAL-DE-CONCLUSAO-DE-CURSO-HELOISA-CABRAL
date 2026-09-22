package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.presentation.ProfessorController;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfessorRequestContractTest {
    private static Validator validator;

    @BeforeAll
    static void setUp() { validator = Validation.buildDefaultValidatorFactory().getValidator(); }

    @Test
    void requiredFieldsAndEmailAreValidatedOnCreation() {
        var valid = new ProfessorController.CreateProfessorRequest("Maria", "maria@example.com", LocalDate.now().minusYears(30), "CREF-123", "secret");
        var missing = new ProfessorController.CreateProfessorRequest(null, "invalid", null, null, null);
        assertTrue(validator.validate(valid).isEmpty());
        assertFalse(validator.validate(missing).isEmpty());
    }

    @Test
    void requiredFieldsAndEmailAreValidatedOnUpdate() {
        var valid = new ProfessorController.UpdateProfessorRequest("Maria", "maria@example.com", LocalDate.now().minusYears(30), "CREF-123");
        var missing = new ProfessorController.UpdateProfessorRequest("", "invalid", null, "");
        assertTrue(validator.validate(valid).isEmpty());
        assertFalse(validator.validate(missing).isEmpty());
    }
}
