package io.arconia.core.bootstrap.autoconfigure;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.core.bootstrap.BootstrapConfigurationFile;
import io.arconia.core.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the hand-maintained configuration metadata of the bootstrap properties in sync with the code.
 * The metadata cannot be generated, since the bootstrap properties are not bound to
 * {@code @ConfigurationProperties} classes.
 */
class BootstrapConfigurationMetadataTests {

    // Read from the sources, since other jars on the test classpath have a metadata file with the same name.
    private static final Path METADATA_FILE = Path.of("src/main/resources/META-INF/spring-configuration-metadata.json");

    private static DocumentContext metadata;

    @BeforeAll
    static void readMetadata() throws IOException {
        metadata = JsonPath.parse(METADATA_FILE.toFile());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
        BootstrapMode.clear();
    }

    @Test
    void shouldDescribeAllBootstrapProperties() {
        List<String> names = metadata.read("$.properties[*].name");

        assertThat(names).containsExactlyInAnyOrder(
                BootstrapMode.PROPERTY_KEY,
                BootstrapConfigurationFile.PROFILES_ENABLED_PROPERTY,
                BootstrapConfigurationFile.DEV_PROFILES_PROPERTY,
                BootstrapConfigurationFile.TEST_PROFILES_PROPERTY);
    }

    @Test
    void shouldDescribeTheTypes() {
        assertThat(property(BootstrapMode.PROPERTY_KEY, "type")).isEqualTo(BootstrapMode.class.getName());
        assertThat(property(BootstrapConfigurationFile.PROFILES_ENABLED_PROPERTY, "type")).isEqualTo(Boolean.class.getName());
        assertThat(property(BootstrapConfigurationFile.DEV_PROFILES_PROPERTY, "type")).isEqualTo("java.util.List<java.lang.String>");
        assertThat(property(BootstrapConfigurationFile.TEST_PROFILES_PROPERTY, "type")).isEqualTo("java.util.List<java.lang.String>");
    }

    @ParameterizedTest
    @CsvSource({ "dev, arconia.dev.profiles", "test, arconia.test.profiles" })
    void shouldDescribeTheDefaultsApplied(String mode, String profilesProperty) {
        System.setProperty(BootstrapMode.PROPERTY_KEY, mode);
        BootstrapMode.clear();
        var environment = new MockEnvironment();

        new BootstrapEnvironmentPostProcessor(Supplier::get).postProcessEnvironment(environment, new SpringApplication());

        // Profiles are enabled by default, so the default profiles of the mode are activated.
        assertThat(property(BootstrapConfigurationFile.PROFILES_ENABLED_PROPERTY, "defaultValue")).isEqualTo(true);
        assertThat(property(profilesProperty, "defaultValue")).isEqualTo(List.of(environment.getActiveProfiles()));
    }

    private static Object property(String name, String attribute) {
        List<Object> values = metadata.read("$.properties[?(@.name == '%s')].%s".formatted(name, attribute));
        assertThat(values).as("'%s' of '%s'", attribute, name).hasSize(1);
        return values.get(0);
    }

}
