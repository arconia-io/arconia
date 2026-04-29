package io.arconia.observation.openllmetry.autoconfigure;

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
 * Integration tests for {@link OpenLLMetryEnvironmentPostProcessor}.
 */
class OpenLLMetryEnvironmentPostProcessorIT {

    private static final String PREFIX = OpenLLMetryProperties.CONFIG_PREFIX;

    private SpringApplication application;

    @BeforeEach
    void setup() {
        this.application = new SpringApplication(Config.class);
        this.application.setWebApplicationType(WebApplicationType.NONE);
    }

    @Test
    void runWhenHasOpenLLMetryPropertiesInSystemPropertiesShouldConvertToArconiaProperties() {
        System.setProperty("TRACELOOP_TRACE_CONTENT", "false");

        try {
            ConfigurableApplicationContext context = this.application.run();

            assertThat(context.getEnvironment().getProperty(PREFIX + ".trace-content")).isEqualTo("false");
        }
        finally {
            System.clearProperty("TRACELOOP_TRACE_CONTENT");
        }
    }

    @Test
    void runWhenHasOpenLLMetryPropertiesInEnvironmentVariablesShouldConvertToArconiaProperties() {
        ConfigurableEnvironment environment = new StandardEnvironment();
        Map<String, Object> envVars = new HashMap<>();
        envVars.put("TRACELOOP_TRACE_CONTENT", "false");
        environment.getPropertySources()
            .addFirst(new MapPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, envVars));
        this.application.setEnvironment(environment);

        ConfigurableApplicationContext context = this.application.run();

        assertThat(context.getEnvironment().getProperty(PREFIX + ".trace-content")).isEqualTo("false");
    }

    @Test
    void runWhenHasOpenLLMetryPropertiesInCommandLineArgumentsShouldConvertToArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--TRACELOOP_TRACE_CONTENT=false"
        );

        assertThat(context.getEnvironment().getProperty(PREFIX + ".trace-content")).isEqualTo("false");
    }

    @Test
    void runWhenHasOpenLLMetryPropertiesInApplicationPropertiesShouldConvertToArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--spring.config.location=classpath:application-openllmetry.properties"
        );

        assertThat(context.getEnvironment().getProperty(PREFIX + ".trace-content")).isEqualTo("false");
    }

    @Test
    void runWhenHasOpenLLMetryPropertiesInApplicationYamlShouldConvertToArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run(
            "--spring.config.location=classpath:application-openllmetry.yml"
        );

        assertThat(context.getEnvironment().getProperty(PREFIX + ".trace-content")).isEqualTo("false");
    }

    @Test
    void runWhenHasNoOpenLLMetryPropertiesShouldNotSetArconiaProperties() {
        ConfigurableApplicationContext context = this.application.run();

        assertThat(context.getEnvironment().getProperty(PREFIX + ".trace-content")).isNull();
    }

    static class Config {

    }

}
