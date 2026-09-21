package devheloisacabral.tcc.com.esportiva;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoundationStructureTest {

    private final Path projectRoot = Path.of("").toAbsolutePath();

    @Test
    void shouldDefineJava21SpringBootAndRequiredFoundationDependencies() throws Exception {
        String pom = Files.readString(projectRoot.resolve("pom.xml"));

        assertTrue(pom.contains("<version>4.1.1</version>"));
        assertTrue(pom.contains("<java.version>21</java.version>"));
        assertTrue(pom.contains("spring-boot-starter-webmvc"));
        assertTrue(pom.contains("spring-boot-starter-validation"));
        assertTrue(pom.contains("spring-boot-starter-data-jpa"));
        assertTrue(pom.contains("spring-boot-starter-flyway"));
        assertTrue(pom.contains("flyway-database-postgresql"));
        assertTrue(pom.contains("<artifactId>postgresql</artifactId>"));
        assertTrue(Files.exists(projectRoot.resolve("mvnw.cmd")));
        assertTrue(Files.exists(projectRoot.resolve("src/main/java/devheloisacabral/tcc/com/esportiva/EsportivaApplication.java")));
        assertTrue(Files.exists(projectRoot.resolve("src/main/java/devheloisacabral/tcc/com/esportiva/domain")));
        assertTrue(Files.exists(projectRoot.resolve("src/main/java/devheloisacabral/tcc/com/esportiva/persistence")));
        assertTrue(Files.exists(projectRoot.resolve("src/main/java/devheloisacabral/tcc/com/esportiva/presentation")));
    }

    @Test
    void shouldReadDatabaseConfigurationOnlyFromEnvironmentVariables() throws Exception {
        String properties = Files.readString(projectRoot.resolve("src/main/resources/application.properties"));

        assertTrue(properties.contains("spring.datasource.url=${DB_URL}"));
        assertTrue(properties.contains("spring.datasource.username=${DB_USERNAME}"));
        assertTrue(properties.contains("spring.datasource.password=${DB_PASSWORD}"));
        assertTrue(properties.contains("server.port=8081"));
        assertFalse(properties.contains("postgresql://user:password@"));
        assertFalse(properties.contains("jdbc:postgresql://user:password@"));
    }

    @Test
    void shouldUseFlywayAndPreventHibernateSchemaChanges() throws Exception {
        String properties = Files.readString(projectRoot.resolve("src/main/resources/application.properties"));

        assertTrue(properties.contains("spring.flyway.enabled=true"));
        assertTrue(properties.contains("spring.flyway.locations=classpath:db/migration"));
        assertTrue(properties.contains("spring.jpa.hibernate.ddl-auto=validate"));
        assertTrue(Files.exists(projectRoot.resolve("src/main/resources/db/migration/V1__create_users_table.sql")));
    }

    @Test
    void shouldNotIncludeLoginOrTokenFlowsInTheFoundation() throws Exception {
        String source = Files.readString(projectRoot.resolve("src/main/java/devheloisacabral/tcc/com/esportiva/EsportivaApplication.java"));
        String presentation = Files.readString(projectRoot.resolve("src/main/java/devheloisacabral/tcc/com/esportiva/presentation/HealthController.java"));

        assertFalse(source.contains("login"));
        assertFalse(source.contains("token"));
        assertFalse(presentation.contains("login"));
        assertFalse(presentation.contains("token"));
    }
}
