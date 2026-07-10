package io.arconia.boot.autoconfigure.bootstrap;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MisplacedBootstrapPropertiesEnvironmentPostProcessor}.
 */
@ExtendWith(OutputCaptureExtension.class)
class MisplacedBootstrapPropertiesEnvironmentPostProcessorTests {

    private static final String CONFIG_FILE_SOURCE_NAME = "Config resource 'class path resource [application.yml]' via location 'optional:classpath:/'";

    private final MisplacedBootstrapPropertiesEnvironmentPostProcessor processor = new MisplacedBootstrapPropertiesEnvironmentPostProcessor();

    @Test
    void shouldWarnWhenProfilesPropertyInApplicationConfiguration(CapturedOutput output) {
        var environment = environmentWithConfigFileProperties(Map.of("arconia.dev.profiles", "custom-dev"));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(output).contains("The 'arconia.dev.profiles' property was found in");
    }

    @Test
    void shouldWarnWhenEnabledPropertyInApplicationConfiguration(CapturedOutput output) {
        var environment = environmentWithConfigFileProperties(Map.of("arconia.bootstrap.profiles.enabled", "false"));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(output).contains("The 'arconia.bootstrap.profiles.enabled' property was found in");
    }

    @Test
    void shouldWarnWhenIndexedProfilesPropertyInApplicationConfiguration(CapturedOutput output) {
        var environment = environmentWithConfigFileProperties(Map.of("arconia.dev.profiles[0]", "custom-dev"));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(output).contains("The 'arconia.dev.profiles' property was found in");
    }

    @Test
    void shouldNotWarnWhenPropertyInOtherSources(CapturedOutput output) {
        var environment = new MockEnvironment().withProperty("arconia.dev.profiles", "custom-dev");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(output).doesNotContain("arconia.dev.profiles");
    }

    @Test
    void shouldNotWarnWhenNothingIsSet(CapturedOutput output) {
        processor.postProcessEnvironment(new MockEnvironment(), new SpringApplication());

        assertThat(output).doesNotContain("was found in");
    }

    @Test
    void shouldWarnWhenModePropertyInApplicationConfiguration(CapturedOutput output) {
        var environment = new MockEnvironment().withProperty("arconia.bootstrap.mode", "dev");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(output).contains("The 'arconia.bootstrap.mode' property was found in an unsupported location");
    }

    @Test
    void shouldWarnWhenModePropertyInBootstrapConfigurationFile(CapturedOutput output) {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource(
                BootstrapEnvironmentPostProcessor.PROPERTY_SOURCE_NAME, Map.of("arconia.bootstrap.mode", "dev")));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(output).contains("The 'arconia.bootstrap.mode' property was found in an unsupported location");
    }

    private static StandardEnvironment environmentWithConfigFileProperties(Map<String, Object> properties) {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource(CONFIG_FILE_SOURCE_NAME, properties));
        return environment;
    }

}
