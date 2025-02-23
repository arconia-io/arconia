package io.arconia.opentelemetry.autoconfigure.exporter.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import io.arconia.opentelemetry.autoconfigure.exporter.OpenTelemetryExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkProperty;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkPropertyAdapter;

/**
 * Converts OpenTelemetry SDK Autoconfigure properties to Arconia properties.
 */
public class OpenTelemetryExporterEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> arconiaProperties = new HashMap<>();
        Stream.of(OtelSdkExporterProperty.values())
                .filter(OtelSdkExporterProperty::isAutomaticConversion)
                .forEach(property -> OtelSdkPropertyAdapter.setProperty(property, environment, arconiaProperties));

        OtelSdkPropertyAdapter.setMapProperty(OtelSdkExporterProperty.EXPORTER_OTLP_HEADERS, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterProtocol(OtelSdkExporterProperty.EXPORTER_OTLP_PROTOCOL, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterCompression(OtelSdkExporterProperty.EXPORTER_OTLP_COMPRESSION, environment, arconiaProperties);

        MapPropertySource mapPropertySource = new MapPropertySource("Arconia OpenTelemetry Exporter", arconiaProperties);
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        mutablePropertySources.addFirst(mapPropertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    enum OtelSdkExporterProperty implements OtelSdkProperty {

        EXPORTER_OTLP_PROTOCOL("otel.exporter.otlp.protocol", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.protocol", false),
        EXPORTER_OTLP_ENDPOINT("otel.exporter.otlp.endpoint", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.endpoint", true),
        EXPORTER_OTLP_HEADERS("otel.exporter.otlp.headers", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.headers", false),
        EXPORTER_OTLP_COMPRESSION("otel.exporter.otlp.compression", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.compression", false),
        EXPORTER_OTLP_TIMEOUT("otel.exporter.otlp.timeout", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".otlp.timeout", true),
        JAVA_EXPORTER_MEMORY_MODE("otel.java.exporter.memory_mode", OpenTelemetryExporterProperties.CONFIG_PREFIX + ".memoryMode", true);

        private final String otelSdkPropertyKey;
        private final String arconiaPropertyKey;
        private final boolean automaticConversion;

        OtelSdkExporterProperty(String otelSdkPropertyKey, String arconiaPropertyKey, boolean automaticConversion) {
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
