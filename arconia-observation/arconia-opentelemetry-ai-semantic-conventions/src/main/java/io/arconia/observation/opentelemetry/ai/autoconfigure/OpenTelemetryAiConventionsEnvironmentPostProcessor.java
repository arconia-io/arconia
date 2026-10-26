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
 * OpenTelemetry defaults to the conservative OTel baseline, capturing no content.
 * OpenLLMetry, OpenLIT, and LangSmith capture all content by default, each recording it
 * where its platform expects it.
 */
class OpenTelemetryAiConventionsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "arconia-opentelemetry-ai-defaults";

    private static final String FLAVOUR_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".flavor";

    private static final String CAPTURE_MESSAGE_CONTENT_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".capture-message-content";

    private static final String CAPTURE_TOOL_DEFINITIONS_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".capture-tool-definitions";

    private static final String CAPTURE_TOOL_CALL_CONTENT_KEY =
            OpenTelemetryAiConventionsProperties.CONFIG_PREFIX + ".capture-tool-call-content";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Assert.notNull(environment, "environment cannot be null");

        String flavor = environment.getProperty(FLAVOUR_KEY, "opentelemetry").toLowerCase();

        Map<String, Object> defaults = switch (flavor) {
            case "openllmetry", "openlit", "langsmith" -> Map.of(
                    CAPTURE_MESSAGE_CONTENT_KEY, "true",
                    CAPTURE_TOOL_DEFINITIONS_KEY, "true",
                    CAPTURE_TOOL_CALL_CONTENT_KEY, "true"
            );
            default -> Map.of(
                    CAPTURE_MESSAGE_CONTENT_KEY, "false",
                    CAPTURE_TOOL_DEFINITIONS_KEY, "false",
                    CAPTURE_TOOL_CALL_CONTENT_KEY, "false"
            );
        };

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
