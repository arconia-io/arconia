package io.arconia.opentelemetry.autoconfigure.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ReplacedSpringBootPropertiesApplicationListener}.
 */
@ExtendWith(OutputCaptureExtension.class)
class ReplacedSpringBootPropertiesApplicationListenerTests {

    private static final String WARNING = "The following Spring Boot properties have no effect";

    private final ReplacedSpringBootPropertiesApplicationListener listener = new ReplacedSpringBootPropertiesApplicationListener();

    @Test
    void listenerIsRegistered() {
        assertThat(SpringFactoriesLoader.loadFactories(ApplicationListener.class, getClass().getClassLoader()))
            .hasAtLeastOneElementOfType(ReplacedSpringBootPropertiesApplicationListener.class);
    }

    @Test
    void shouldWarnWhenOpenTelemetryPropertyIsSet(CapturedOutput output) {
        var environment = new MockEnvironment()
            .withProperty("management.opentelemetry.tracing.export.otlp.endpoint", "http://localhost:4318/v1/traces");

        listener.onApplicationEvent(preparedEvent(environment));

        assertThat(output).contains(WARNING)
            .contains("[management.opentelemetry.tracing.export.otlp.endpoint]")
            .contains("Use the 'arconia.otel.*' properties instead.");
    }

    @Test
    void shouldWarnWhenOtlpPropertyIsSet(CapturedOutput output) {
        var environment = new MockEnvironment()
            .withProperty("management.otlp.metrics.export.url", "http://localhost:4318/v1/metrics");

        listener.onApplicationEvent(preparedEvent(environment));

        assertThat(output).contains(WARNING).contains("[management.otlp.metrics.export.url]");
    }

    @Test
    void shouldWarnWhenPropertyIsSetAsEnvironmentVariable(CapturedOutput output) {
        var environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                Map.of("MANAGEMENT_OTLP_METRICS_EXPORT_URL", "http://localhost:4318/v1/metrics")));

        listener.onApplicationEvent(preparedEvent(environment));

        assertThat(output).contains(WARNING).contains("[management.otlp.metrics.export.url]");
    }

    @Test
    void shouldWarnOnceWithAllTheProperties(CapturedOutput output) {
        var environment = new MockEnvironment()
            .withProperty("management.otlp.metrics.export.url", "http://localhost:4318/v1/metrics")
            .withProperty("management.opentelemetry.resource-attributes.cluster", "local");
        environment.getPropertySources().addLast(new SystemEnvironmentPropertySource(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                Map.of("MANAGEMENT_OTLP_METRICS_EXPORT_URL", "http://localhost:4318/v1/metrics")));

        listener.onApplicationEvent(preparedEvent(environment));

        assertThat(output.getOut().split(WARNING, -1)).hasSize(2);
        assertThat(output).contains("[management.opentelemetry.resource-attributes.cluster, management.otlp.metrics.export.url]");
    }

    @Test
    void shouldNotWarnWhenOtherManagementPropertiesAreSet(CapturedOutput output) {
        var environment = new MockEnvironment()
            .withProperty("management.tracing.sampling.probability", "1.0")
            .withProperty("management.tracing.export.enabled", "false")
            .withProperty("management.defaults.metrics.export.enabled", "false")
            .withProperty("management.logging.export.otlp.enabled", "false");

        listener.onApplicationEvent(preparedEvent(environment));

        assertThat(output).doesNotContain(WARNING);
    }

    @Test
    void shouldNotWarnWhenNothingIsSet(CapturedOutput output) {
        listener.onApplicationEvent(preparedEvent(new MockEnvironment()));

        assertThat(output).doesNotContain(WARNING);
    }

    @Test
    void shouldNotWarnWhenOpenTelemetryIsDisabled(CapturedOutput output) {
        var environment = new MockEnvironment()
            .withProperty("arconia.otel.enabled", "false")
            .withProperty("management.otlp.metrics.export.url", "http://localhost:4318/v1/metrics");

        listener.onApplicationEvent(preparedEvent(environment));

        assertThat(output).doesNotContain(WARNING);
    }

    private static ApplicationPreparedEvent preparedEvent(ConfigurableEnvironment environment) {
        var applicationContext = new GenericApplicationContext();
        applicationContext.setEnvironment(environment);
        return new ApplicationPreparedEvent(new SpringApplication(), new String[0], applicationContext);
    }

}
