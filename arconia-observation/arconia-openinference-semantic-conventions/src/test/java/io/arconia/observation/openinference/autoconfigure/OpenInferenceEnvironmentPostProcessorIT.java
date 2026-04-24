package io.arconia.observation.openinference.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenInferenceEnvironmentPostProcessor}.
 */
class OpenInferenceEnvironmentPostProcessorIT {

    private static final String PREFIX = OpenInferenceProperties.CONFIG_PREFIX + ".options";

    private SpringApplication application;

    @BeforeEach
    void setup() {
        this.application = new SpringApplication(Config.class);
        this.application.setWebApplicationType(WebApplicationType.NONE);
    }

    @Test
    void runWhenHasOpenInferencePropertiesInSystemPropertiesShouldConvertToArconiaProperties() {
        System.setProperty("OPENINFERENCE_HIDE_INPUTS", "true");
        System.setProperty("OPENINFERENCE_HIDE_OUTPUTS", "true");
        System.setProperty("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "64000");

        try {
            ConfigurableApplicationContext context = this.application.run();

            assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-inputs")).isEqualTo("true");
            assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-outputs")).isEqualTo("true");
            assertThat(context.getEnvironment().getProperty(PREFIX + ".base64-image-max-length")).isEqualTo("64000");
        }
        finally {
            System.clearProperty("OPENINFERENCE_HIDE_INPUTS");
            System.clearProperty("OPENINFERENCE_HIDE_OUTPUTS");
            System.clearProperty("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH");
        }
    }

    @Test
    void runWhenHasOpenInferencePropertiesInEnvironmentVariablesShouldConvertToArconiaProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        Map<String, Object> envVars = new HashMap<>();
        envVars.put("OPENINFERENCE_HIDE_INPUTS", "true");
        envVars.put("OPENINFERENCE_HIDE_OUTPUTS", "true");
        envVars.put("OPENINFERENCE_HIDE_INPUT_MESSAGES", "true");
        envVars.put("OPENINFERENCE_HIDE_OUTPUT_MESSAGES", "true");
        envVars.put("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "64000");
        environment.getPropertySources()
            .addFirst(new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, envVars));
        this.application.setEnvironment(environment);

        ConfigurableApplicationContext context = this.application.run();

        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-inputs")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-outputs")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-input-messages")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-output-messages")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".base64-image-max-length")).isEqualTo("64000");
    }

    @Test
    void runWhenHasOpenInferencePropertiesInCommandLineArgumentsShouldConvertToArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--OPENINFERENCE_HIDE_INPUTS=true",
            "--OPENINFERENCE_HIDE_OUTPUTS=true",
            "--OPENINFERENCE_HIDE_CHOICES=true",
            "--OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH=64000"
        );

        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-inputs")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-outputs")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-choices")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".base64-image-max-length")).isEqualTo("64000");
    }

    @Test
    void runWhenHasOpenInferencePropertiesInApplicationPropertiesShouldConvertToArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--spring.config.location=classpath:application-openinference.properties"
        );

        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-inputs")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-outputs")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-input-messages")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-output-messages")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-input-images")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-input-text")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-output-text")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-prompts")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-choices")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-llm-invocation-parameters")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-embeddings-text")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-embeddings-vectors")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".base64-image-max-length")).isEqualTo("64000");
    }

    @Test
    void runWhenHasOpenInferencePropertiesInApplicationYamlShouldConvertToArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--spring.config.location=classpath:application-openinference.yml"
        );

        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-inputs")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-outputs")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-input-messages")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-output-messages")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-input-images")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-input-text")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-output-text")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-prompts")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-choices")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-llm-invocation-parameters")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-embeddings-text")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-embeddings-vectors")).isEqualTo("true");
        assertThat(context.getEnvironment().getProperty(PREFIX + ".base64-image-max-length")).isEqualTo("64000");
    }

    @Test
    void runWhenHasNoOpenInferencePropertiesShouldNotSetArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run();

        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-inputs")).isNull();
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-outputs")).isNull();
        assertThat(context.getEnvironment().getProperty(PREFIX + ".base64-image-max-length")).isNull();
    }

    @Test
    void runWhenHasInvalidIntegerPropertyShouldNotConvertIt() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        Map<String, Object> envVars = new HashMap<>();
        envVars.put("OPENINFERENCE_BASE64_IMAGE_MAX_LENGTH", "not-a-number");
        envVars.put("OPENINFERENCE_HIDE_INPUTS", "true");
        environment.getPropertySources()
            .addFirst(new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, envVars));
        this.application.setEnvironment(environment);

        ConfigurableApplicationContext context = this.application.run();

        assertThat(context.getEnvironment().getProperty(PREFIX + ".base64-image-max-length")).isNull();
        assertThat(context.getEnvironment().getProperty(PREFIX + ".hide-inputs")).isEqualTo("true");
    }

    static class Config {

    }

}
