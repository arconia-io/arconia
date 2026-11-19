package io.arconia.opentelemetry.test;

import java.util.List;

import io.opentelemetry.sdk.logs.export.LogRecordExporter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterAutoConfiguration;
import io.arconia.opentelemetry.test.OpenTelemetryLoggingExportContextCustomizerFactory.LoggingExportContextCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link OpenTelemetryLoggingExportContextCustomizerFactory}.
 */
class OpenTelemetryLoggingExportContextCustomizerFactoryTests {

    private static final String LOGGING_EXPORT = "management.logging.export.enabled";

    private final OpenTelemetryLoggingExportContextCustomizerFactory factory = new OpenTelemetryLoggingExportContextCustomizerFactory();

    private final GenericApplicationContext context = new GenericApplicationContext();

    @Test
    void registeredAsContextCustomizerFactory() {
        List<ContextCustomizerFactory> factories = SpringFactoriesLoader
                .loadFactories(ContextCustomizerFactory.class, getClass().getClassLoader());

        assertThat(factories).hasAtLeastOneElementOfType(OpenTelemetryLoggingExportContextCustomizerFactory.class);
    }

    @Test
    void exportDisabledByDefault() {
        assertThat(customize().getProperty(LOGGING_EXPORT)).isEqualTo("false");
    }

    @Test
    void exportDisabledEvenWhenEnabledInTheApplication() {
        TestPropertyValues.of(LOGGING_EXPORT + "=true").applyTo(context);

        assertThat(customize().getProperty(LOGGING_EXPORT)).isEqualTo("false");
    }

    @Test
    void exportRequested() {
        TestPropertyValues.of("spring.test.logging.export=true").applyTo(context);

        ConfigurableEnvironment environment = customize();

        assertThat(environment.containsProperty(LOGGING_EXPORT)).isFalse();
        assertThat(environment.getPropertySources().contains(LoggingExportContextCustomizer.PROPERTY_SOURCE_NAME)).isFalse();
    }

    @Test
    void customizersAreEqualSoThatApplicationContextsAreShared() {
        ContextCustomizer first = factory.createContextCustomizer(ExportDisabled.class, List.of());
        ContextCustomizer second = factory.createContextCustomizer(ExportRequested.class, List.of());

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(mock(ContextCustomizer.class));
    }

    private ConfigurableEnvironment customize() {
        factory.createContextCustomizer(ExportDisabled.class, List.of())
                .customizeContext(context, mock(MergedContextConfiguration.class));
        return context.getEnvironment();
    }

    @Nested
    @SpringJUnitConfig
    @ImportAutoConfiguration({ OpenTelemetryAutoConfiguration.class, OpenTelemetryExporterAutoConfiguration.class,
            OpenTelemetryLoggingExporterAutoConfiguration.class })
    @TestPropertySource(properties = "arconia.otel.logs.exporter.type=console")
    class ExportDisabled {

        @Autowired
        Environment environment;

        @Autowired
        ObjectProvider<LogRecordExporter> logRecordExporters;

        @Test
        void logsAreNotExported() {
            assertThat(environment.getProperty(LOGGING_EXPORT)).isEqualTo("false");
            assertThat(logRecordExporters.stream()).isEmpty();
        }

    }

    @Nested
    @SpringJUnitConfig
    @ImportAutoConfiguration({ OpenTelemetryAutoConfiguration.class, OpenTelemetryExporterAutoConfiguration.class,
            OpenTelemetryLoggingExporterAutoConfiguration.class })
    @TestPropertySource(properties = { "arconia.otel.logs.exporter.type=console", "spring.test.logging.export=true" })
    class ExportRequested {

        @Autowired
        ObjectProvider<LogRecordExporter> logRecordExporters;

        @Test
        void logsAreExported() {
            assertThat(logRecordExporters.stream()).hasSize(1);
        }

    }

}
