package io.arconia.observation.opentelemetry.ai.autoconfigure;

import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.Assert;

/**
 * Injects flavor-specific default property values at the lowest priority so that
 * any user-configured value always takes precedence.
 * <p>
 * OpenTelemetry defaults to the conservative OTel baseline (NONE, no tool definitions).
 * OpenLLMetry defaults to SPAN_ATTRIBUTES with tool definitions included.
 * LangSmith defaults to SPAN_EVENTS (content is emitted via span events by LangSmith handlers)
 * with tool definitions included.
 */
class OpenTelemetryAiConventionsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "arconia-opentelemetry-ai-defaults";

    private static final String FLAVOUR_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".flavor";

    private static final String CAPTURE_CONTENT_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".capture-content";

    private static final String INCLUDE_TOOL_DEFINITIONS_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".include-tool-definitions";

    private static final String TOOL_EXECUTION_INCLUDE_CONTENT_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".include-tool-call-content";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");

        String flavor = environment.getProperty(FLAVOUR_KEY, "opentelemetry").toLowerCase();

        Map<String, Object> defaults = switch (flavor) {
            case "openllmetry" -> Map.of(
                    CAPTURE_CONTENT_KEY, "SPAN_ATTRIBUTES",
                    INCLUDE_TOOL_DEFINITIONS_KEY, "true",
                    TOOL_EXECUTION_INCLUDE_CONTENT_KEY, "true"
            );
            case "langsmith" -> Map.of(
                    CAPTURE_CONTENT_KEY, "SPAN_EVENTS",
                    INCLUDE_TOOL_DEFINITIONS_KEY, "true",
                    TOOL_EXECUTION_INCLUDE_CONTENT_KEY, "true"
            );
            default -> Map.of(
                    CAPTURE_CONTENT_KEY, "NONE",
                    INCLUDE_TOOL_DEFINITIONS_KEY, "false",
                    TOOL_EXECUTION_INCLUDE_CONTENT_KEY, "false"
            );
        };

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
