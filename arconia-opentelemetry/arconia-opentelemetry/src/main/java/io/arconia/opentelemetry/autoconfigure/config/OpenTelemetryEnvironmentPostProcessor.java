package io.arconia.opentelemetry.autoconfigure.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.Assert;

/**
 * Converts OpenTelemetry Environment Variable Specification properties to Arconia properties
 */
class OpenTelemetryEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "opentelemetry-environment-variable-specification";

    private final Log logger;

    /**
     * The logger is deferred until the logging system is initialized, since any message
     * logged before that would be lost.
     */
    OpenTelemetryEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.logger = logFactory.getLog(OpenTelemetryEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");

        Boolean enabled = environment.getProperty("arconia.otel.compatibility.environment-variable-specification", Boolean.class, true);
        if (!enabled) {
            return;
        }

        // The order matters: more specific properties are mapped after the general ones, so they take precedence.
        var adapter = new OpenTelemetryEnvironmentPropertyAdapter(environment, logger);
        OpenTelemetryEnvironmentPropertyAdapters.general(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.batchSpanProcessor(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.logRecordProcessor(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.attributeLimits(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.spanLimits(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.logRecordLimits(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.exporterSelection(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.metrics(adapter);
        OpenTelemetryEnvironmentPropertyAdapters.otlpExporter(adapter);

        environment.getPropertySources().addFirst(adapter.toPropertySource(PROPERTY_SOURCE_NAME));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
