package devheloisacabral.tcc.com.esportiva;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRole;
import devheloisacabral.tcc.com.esportiva.domain.auth.UserRoleConverter;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthUserMappingTest {

    @Test
    void shouldMapAuthenticationUserWithoutPlainTextPasswordColumn() throws Exception {
        assertTrue(AuthUser.class.isAnnotationPresent(Entity.class));
        assertEquals("users", AuthUser.class.getAnnotation(Table.class).name());

        Field email = AuthUser.class.getDeclaredField("email");
        Field passwordHash = AuthUser.class.getDeclaredField("passwordHash");
        Field role = AuthUser.class.getDeclaredField("role");

        assertEquals("", email.getAnnotation(Column.class).name());
        assertEquals("password_hash", passwordHash.getAnnotation(Column.class).name());
        assertEquals("", role.getAnnotation(Column.class).name());
        assertTrue(email.getAnnotation(Column.class).unique());
        assertFalse(email.getAnnotation(Column.class).nullable());
        assertFalse(role.getAnnotation(Column.class).nullable());
        assertFalse(hasFieldNamed("password"));
    }

    @Test
    void shouldConvertOnlyTheDefinedRolesToDatabaseValues() {
        UserRoleConverter converter = new UserRoleConverter();

        assertEquals("admin", converter.convertToDatabaseColumn(UserRole.ADMIN));
        assertEquals("professor", converter.convertToDatabaseColumn(UserRole.PROFESSOR));
        assertEquals("aluno", converter.convertToDatabaseColumn(UserRole.ALUNO));
        assertEquals(UserRole.ADMIN, converter.convertToEntityAttribute("admin"));
        assertEquals(UserRole.PROFESSOR, converter.convertToEntityAttribute("professor"));
        assertEquals(UserRole.ALUNO, converter.convertToEntityAttribute("aluno"));
    }

    @Test
    void shouldDefineEmailUniquenessAndAllowedRolesInMigration() throws Exception {
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V1__create_users_table.sql"));

        assertTrue(migration.contains("email VARCHAR(320) NOT NULL"));
        assertTrue(migration.contains("CONSTRAINT uk_users_email UNIQUE (email)"));
        assertTrue(migration.contains("password_hash VARCHAR(255) NOT NULL"));
        assertTrue(migration.contains("CONSTRAINT ck_users_role CHECK (role IN ('admin', 'professor', 'aluno'))"));
        assertFalse(migration.contains("password VARCHAR"));
    }

    private boolean hasFieldNamed(String fieldName) {
        for (Field field : AuthUser.class.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }
}
