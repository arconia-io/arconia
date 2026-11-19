package io.arconia.opentelemetry.test;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/**
 * Turns off the export of logs in tests, so that they don't send telemetry to a backend
 * that is usually not there. A test turns it back on with the {@code spring.test.logging.export} property.
 * <p>
 * Spring Boot does the same for metrics and traces in its test modules, but not for logs.
 */
class OpenTelemetryLoggingExportContextCustomizerFactory implements ContextCustomizerFactory {

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        return new LoggingExportContextCustomizer();
    }

    static final class LoggingExportContextCustomizer implements ContextCustomizer {

        static final String PROPERTY_SOURCE_NAME = "arconia-opentelemetry-test";

        @Override
        public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
            ConfigurableEnvironment environment = context.getEnvironment();
            if (!environment.getProperty("spring.test.logging.export", Boolean.class, false)) {
                environment.getPropertySources()
                        .addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of("management.logging.export.enabled", "false")));
            }
        }

        // The customizer has no state, so all its instances share the cached application contexts.

        @Override
        public boolean equals(@Nullable Object other) {
            return other != null && getClass() == other.getClass();
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

    }

}
