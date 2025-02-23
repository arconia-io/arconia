package io.arconia.opentelemetry.autoconfigure.sdk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

import io.arconia.opentelemetry.autoconfigure.OpenTelemetryProperties;

/**
 * Converts OpenTelemetry SDK Autoconfigure properties to Arconia properties.
 */
public class OpenTelemetryEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> arconiaProperties = new HashMap<>();
        Stream.of(OtelSdkGeneralProperty.values())
                .filter(OtelSdkGeneralProperty::isAutomaticConversion)
                .forEach(property -> OtelSdkPropertyAdapter.setProperty(property, environment, arconiaProperties));

        setSdkDisabled(OtelSdkGeneralProperty.SDK_DISABLED, environment, arconiaProperties);
        setPropagators(OtelSdkGeneralProperty.PROPAGATORS, environment, arconiaProperties);

        MapPropertySource mapPropertySource = new MapPropertySource("Arconia OpenTelemetry", arconiaProperties);
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        mutablePropertySources.addFirst(mapPropertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * Adapts the OpenTelemetry SDK property for the SDK disabled to the Arconia property.
     */
    static void setSdkDisabled(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());

        if (!StringUtils.hasText(value)) {
            return;
        }

        if (Boolean.parseBoolean(value.toLowerCase().trim())) {
            arconiaProperties.put(property.getArconiaPropertyKey(), false);
        } else {
            arconiaProperties.put(property.getArconiaPropertyKey(), true);
        }
    }

    /**
     * Adapts the OpenTelemetry SDK property for the propagators to the Arconia property.
     */
    static void setPropagators(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (StringUtils.hasText(value)) {
            Set<TracingProperties.Propagation.PropagationType> propagators = new HashSet<>();
            String[] items = value.toLowerCase().trim().split(",");
            for (String item : items) {
                var propagator = switch (item) {
                    case "baggage" -> TracingProperties.Propagation.PropagationType.W3C;
                    case "tracecontext" -> TracingProperties.Propagation.PropagationType.W3C;
                    case "b3" -> TracingProperties.Propagation.PropagationType.B3;
                    case "b3multi" -> TracingProperties.Propagation.PropagationType.B3_MULTI;
                    default -> null;
                };
                if (propagator == null) {
                    logger.warn("Unsupported value for {}: {}", property.getOtelSdkPropertyKey(), value);
                } else {
                    propagators.add(propagator);
                }
            }
            arconiaProperties.put(property.getArconiaPropertyKey(), propagators.stream().toList());
        }
    }

    enum OtelSdkGeneralProperty implements OtelSdkProperty {

        SDK_DISABLED("otel.sdk.disabled", OpenTelemetryProperties.CONFIG_PREFIX + ".enabled", false),
        // Used in Spring Boot Actuator. See OpenTelemetryPropagationConfigurations.
        PROPAGATORS("otel.propagators", "management.tracing.propagation.produce", false);

        private final String otelSdkPropertyKey;
        private final String arconiaPropertyKey;
        private final boolean automaticConversion;

        OtelSdkGeneralProperty(String otelSdkPropertyKey, String arconiaPropertyKey, boolean automaticConversion) {
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
