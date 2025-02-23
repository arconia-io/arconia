package io.arconia.opentelemetry.autoconfigure.resource.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkProperty;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkPropertyAdapter;

/**
 * Converts OpenTelemetry SDK Autoconfigure properties to Arconia properties.
 */
public class OpenTelemetryResourceEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> arconiaProperties = new HashMap<>();
        OtelSdkPropertyAdapter.setMapProperty(OtelSdkResourceProperty.RESOURCE_ATTRIBUTES, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setListProperty(OtelSdkResourceProperty.RESOURCE_DISABLED_KEYS, environment, arconiaProperties);
        Stream.of(OtelSdkResourceProperty.values())
                .filter(OtelSdkResourceProperty::isAutomaticConversion)
                .forEach(property -> OtelSdkPropertyAdapter.setProperty(property, environment, arconiaProperties));
        MapPropertySource mapPropertySource = new MapPropertySource("Arconia OpenTelemetry Resource", arconiaProperties);
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        mutablePropertySources.addFirst(mapPropertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    enum OtelSdkResourceProperty implements OtelSdkProperty {

        RESOURCE_ATTRIBUTES("otel.resource.attributes", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".resource.attributes", false),
        RESOURCE_DISABLED_KEYS("otel.resource.disabled.keys", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".resource.disabled-keys", false),
        SERVICE_NAME("otel.service.name", OpenTelemetryResourceProperties.CONFIG_PREFIX + ".resource.'service-name'", true);

        private final String otelSdkPropertyKey;
        private final String arconiaPropertyKey;
        private final boolean automaticConversion;

        OtelSdkResourceProperty(String otelSdkPropertyKey, String arconiaPropertyKey, boolean automaticConversion) {
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
