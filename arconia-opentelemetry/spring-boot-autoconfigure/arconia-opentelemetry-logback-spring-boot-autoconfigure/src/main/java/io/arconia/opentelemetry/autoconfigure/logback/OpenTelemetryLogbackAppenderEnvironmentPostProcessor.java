package io.arconia.opentelemetry.autoconfigure.logback;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkProperty;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkPropertyAdapter;

/**
 * Converts OpenTelemetry SDK Autoconfigure properties to Arconia properties.
 */
public class OpenTelemetryLogbackAppenderEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> arconiaProperties = new HashMap<>();
        Stream.of(OtelSdkLogbackProperty.values())
                .filter(OtelSdkLogbackProperty::isAutomaticConversion)
                .forEach(property -> OtelSdkPropertyAdapter.setProperty(property, environment, arconiaProperties));

        MapPropertySource mapPropertySource = new MapPropertySource("Arconia OpenTelemetry Instrumentation Logback", arconiaProperties);
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        mutablePropertySources.addFirst(mapPropertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    enum OtelSdkLogbackProperty implements OtelSdkProperty {

        ENABLED("otel.instrumentation.logback-appender.enabled", OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX + ".enabled", true),
        CAPTURE_ARGUMENTS("otel.instrumentation.logback-appender.experimental.capture-arguments", OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX + ".capture-arguments", true),
        CAPTURE_CODE_ATTRIBUTES("otel.instrumentation.logback-appender.experimental.capture-code-attributes", OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX + ".capture-code-attributes", true),
        CAPTURE_EXPERIMENTAL_ATTRIBUTES("otel.instrumentation.logback-appender.experimental-log-attributes", OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX + ".capture-experimental-attributes", true),
        CAPTURE_KEY_VALUE_PAIR_ATTRIBUTES("otel.instrumentation.logback-appender.experimental.capture-key-value-pair-attributes", OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX + ".capture-key-value-pair-attributes", true),
        CAPTURE_LOGGER_CONTEXT("otel.instrumentation.logback-appender.experimental.capture-logger-context-attributes", OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX + ".capture-logger-context", true),
        CAPTURE_LOGSTASH_ATTRIBUTES("otel.instrumentation.logback-appender.experimental.capture-logstash-attributes", OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX + ".capture-logstash-attributes", true),
        CAPTURE_MARKER_ATTRIBUTE("otel.instrumentation.logback-appender.experimental.capture-marker-attribute", OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX + ".capture-marker-attribute", true),
        CAPTURE_MDC_ATTRIBUTES("otel.instrumentation.logback-appender.experimental.capture-mdc-attributes", OpenTelemetryLogbackAppenderProperties.CONFIG_PREFIX + ".capture-mdc-attributes", true);

        private final String otelSdkPropertyKey;
        private final String arconiaPropertyKey;
        private final boolean automaticConversion;

        OtelSdkLogbackProperty(String otelSdkPropertyKey, String arconiaPropertyKey, boolean automaticConversion) {
            this.otelSdkPropertyKey = otelSdkPropertyKey;
            this.arconiaPropertyKey = arconiaPropertyKey;
            this.automaticConversion = automaticConversion;
        }

        @Override
        public String getOtelSdkPropertyKey() {
            return otelSdkPropertyKey;
        }

        @Override
        public String getArconiaPropertyKey() {
            return arconiaPropertyKey;
        }

        @Override
        public boolean isAutomaticConversion() {
            return automaticConversion;
        }

    }

}
