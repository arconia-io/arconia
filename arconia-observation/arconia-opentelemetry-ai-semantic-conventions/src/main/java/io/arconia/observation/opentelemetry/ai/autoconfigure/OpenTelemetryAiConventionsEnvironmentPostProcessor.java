package io.arconia.observation.opentelemetry.ai.autoconfigure;

import java.util.Locale;
import java.util.Map;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.observation.opentelemetry.ai.instrumentation.GenAiConventionFlavor;

/**
 * Injects the default values of the content capturing properties for the configured flavor at the lowest
 * priority, so that any user-configured value takes precedence.
 * <p>
 * Whether a flavor captures content by default is defined by {@link GenAiConventionFlavor}: the OpenTelemetry
 * flavor doesn't, the other flavors do, each recording it where its platform expects it.
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

        String capturesContent = String.valueOf(flavor(environment).capturesContentByDefault());
        Map<String, Object> defaults = Map.of(
                CAPTURE_MESSAGE_CONTENT_KEY, capturesContent,
                CAPTURE_TOOL_DEFINITIONS_KEY, capturesContent,
                CAPTURE_TOOL_CALL_CONTENT_KEY, capturesContent
        );

        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, defaults));
    }

    /**
     * Resolves the configured flavor the way the flavor configurations match it, ignoring case.
     * An unknown value falls back to the OpenTelemetry flavor and its conservative defaults,
     * leaving the binding of the property to report the invalid value.
     */
    private static GenAiConventionFlavor flavor(ConfigurableEnvironment environment) {
        String flavor = environment.getProperty(FLAVOUR_KEY);
        if (!StringUtils.hasText(flavor)) {
            return GenAiConventionFlavor.OPENTELEMETRY;
        }
        try {
            return GenAiConventionFlavor.valueOf(flavor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return GenAiConventionFlavor.OPENTELEMETRY;
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

}
