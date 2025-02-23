package io.arconia.opentelemetry.autoconfigure.logs.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import io.arconia.opentelemetry.autoconfigure.logs.OpenTelemetryLoggingProperties;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.OpenTelemetryLoggingExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkProperty;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkPropertyAdapter;

/**
 * Converts OpenTelemetry SDK Autoconfigure properties to Arconia properties.
 */
public class OpenTelemetryLoggingEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> arconiaProperties = new HashMap<>();
        Stream.of(OtelSdkLoggingProperty.values())
                .filter(OtelSdkLoggingProperty::isAutomaticConversion)
                .forEach(property -> OtelSdkPropertyAdapter.setProperty(property, environment, arconiaProperties));

        OtelSdkPropertyAdapter.setMapProperty(OtelSdkLoggingProperty.EXPORTER_OTLP_LOGS_HEADERS, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterType(OtelSdkLoggingProperty.LOGS_EXPORTER, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterProtocol(OtelSdkLoggingProperty.EXPORTER_OTLP_LOGS_PROTOCOL, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterCompression(OtelSdkLoggingProperty.EXPORTER_OTLP_LOGS_COMPRESSION, environment, arconiaProperties);

        MapPropertySource mapPropertySource = new MapPropertySource("Arconia OpenTelemetry Logging", arconiaProperties);
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        mutablePropertySources.addFirst(mapPropertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    enum OtelSdkLoggingProperty implements OtelSdkProperty {

        BLRP_SCHEDULE_DELAY("otel.blrp.schedule.delay", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.scheduleDelay", true),
        BLRP_MAX_QUEUE_SIZE("otel.blrp.max.queue.size", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.maxQueueSize", true),
        BLRP_MAX_EXPORT_BATCH_SIZE("otel.blrp.max.export.batch.size", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.maxExportBatchSize", true),
        BLRP_EXPORT_TIMEOUT("otel.blrp.export.timeout", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".processor.exporterTimeout", true),

        ATTRIBUTE_VALUE_LENGTH_LIMIT("otel.attribute.value.length.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".logLimits.maxAttributeValueLength", true),
        ATTRIBUTE_COUNT_LIMIT("otel.attribute.count.limit", OpenTelemetryLoggingProperties.CONFIG_PREFIX + ".logLimits.maxNumberOfAttributes", true),

        LOGS_EXPORTER("otel.logs.exporter", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".type", false),
        EXPORTER_OTLP_LOGS_PROTOCOL("otel.exporter.otlp.logs.protocol", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.protocol", false),
        EXPORTER_OTLP_LOGS_ENDPOINT("otel.exporter.otlp.logs.endpoint", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.endpoint", true),
        EXPORTER_OTLP_LOGS_HEADERS("otel.exporter.otlp.logs.headers", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.headers", false),
        EXPORTER_OTLP_LOGS_COMPRESSION("otel.exporter.otlp.logs.compression", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.compression", false),
        EXPORTER_OTLP_LOGS_TIMEOUT("otel.exporter.otlp.logs.timeout", OpenTelemetryLoggingExporterProperties.CONFIG_PREFIX + ".otlp.timeout", true);

        private final String otelSdkPropertyKey;
        private final String arconiaPropertyKey;
        private final boolean automaticConversion;

        OtelSdkLoggingProperty(String otelSdkPropertyKey, String arconiaPropertyKey, boolean automaticConversion) {
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
