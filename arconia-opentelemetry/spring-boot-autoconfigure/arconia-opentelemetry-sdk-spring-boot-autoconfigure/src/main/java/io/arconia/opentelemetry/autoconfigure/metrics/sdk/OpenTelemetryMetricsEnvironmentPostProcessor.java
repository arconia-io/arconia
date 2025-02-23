package io.arconia.opentelemetry.autoconfigure.metrics.sdk;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.util.StringUtils;

import io.arconia.opentelemetry.autoconfigure.metrics.OpenTelemetryMetricsProperties;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.AggregationTemporalityStrategy;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.HistogramAggregationStrategy;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.OpenTelemetryMetricsExporterProperties;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkProperty;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkPropertyAdapter;

/**
 * Converts OpenTelemetry SDK Autoconfigure properties to Arconia properties.
 */
public class OpenTelemetryMetricsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryMetricsEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> arconiaProperties = new HashMap<>();
        Stream.of(OtelSdkMetricsProperty.values())
                .filter(OtelSdkMetricsProperty::isAutomaticConversion)
                .forEach(property -> OtelSdkPropertyAdapter.setProperty(property, environment, arconiaProperties));

        setHistogramAggregation(OtelSdkMetricsProperty.EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION, environment, arconiaProperties);
        setAggregationTemporality(OtelSdkMetricsProperty.EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setMapProperty(OtelSdkMetricsProperty.EXPORTER_OTLP_METRICS_HEADERS, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterType(OtelSdkMetricsProperty.METRICS_EXPORTER, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterProtocol(OtelSdkMetricsProperty.EXPORTER_OTLP_METRICS_PROTOCOL, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterCompression(OtelSdkMetricsProperty.EXPORTER_OTLP_METRICS_COMPRESSION, environment, arconiaProperties);

        MapPropertySource mapPropertySource = new MapPropertySource("Arconia OpenTelemetry Metrics", arconiaProperties);
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        mutablePropertySources.addFirst(mapPropertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * Adapts the OpenTelemetry SDK property for the histogram aggregation to the Arconia property.
     */
    static void setHistogramAggregation(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (StringUtils.hasText(value)) {
            var histogramAggregation = switch (value.toUpperCase().trim()) {
                case "BASE2_EXPONENTIAL_BUCKET_HISTOGRAM" -> HistogramAggregationStrategy.BASE2_EXPONENTIAL_BUCKET_HISTOGRAM;
                case "EXPLICIT_BUCKET_HISTOGRAM" -> HistogramAggregationStrategy.EXPLICIT_BUCKET_HISTOGRAM;
                default -> null;
            };
            if (histogramAggregation == null) {
                logger.warn("Unsupported value for {}: {}", property.getOtelSdkPropertyKey(), value);
            } else {
                arconiaProperties.put(property.getArconiaPropertyKey(), histogramAggregation);
            }
        }
    }

    /**
     * Adapts the OpenTelemetry SDK property for the aggregation temporality to the Arconia property.
     */
    static void setAggregationTemporality(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (StringUtils.hasText(value)) {
            var aggregationTemporality = switch (value.toUpperCase().trim()) {
                case "CUMULATIVE" -> AggregationTemporalityStrategy.CUMULATIVE;
                case "DELTA" -> AggregationTemporalityStrategy.DELTA;
                case "LOWMEMORY" -> AggregationTemporalityStrategy.LOW_MEMORY;
                default -> null;
            };
            if (aggregationTemporality == null) {
                logger.warn("Unsupported value for {}: {}", property.getOtelSdkPropertyKey(), value);
            } else {
                arconiaProperties.put(property.getArconiaPropertyKey(), aggregationTemporality);
            }
        }
    }

    enum OtelSdkMetricsProperty implements OtelSdkProperty {

        METRIC_EXPORT_INTERVAL("otel.metric.export.interval", OpenTelemetryMetricsProperties.CONFIG_PREFIX + ".interval", true),

        METRICS_EXPORTER("otel.metrics.exporter", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".type", false),
        EXPORTER_OTLP_METRICS_PROTOCOL("otel.exporter.otlp.metrics.protocol", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.protocol", false),
        EXPORTER_OTLP_METRICS_ENDPOINT("otel.exporter.otlp.metrics.endpoint", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.endpoint", true),
        EXPORTER_OTLP_METRICS_HEADERS("otel.exporter.otlp.metrics.headers", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.headers", false),
        EXPORTER_OTLP_METRICS_COMPRESSION("otel.exporter.otlp.metrics.compression", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.compression", false),
        EXPORTER_OTLP_METRICS_TIMEOUT("otel.exporter.otlp.metrics.timeout", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".otlp.timeout", true),

        EXPORTER_OTLP_METRICS_DEFAULT_HISTOGRAM_AGGREGATION("otel.exporter.otlp.metrics.default.histogram.aggregation", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".histogram-aggregation", false),
        EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE("otel.exporter.otlp.metrics.temporality.preference", OpenTelemetryMetricsExporterProperties.CONFIG_PREFIX + ".aggregation-temporality", false);

        private final String otelSdkPropertyKey;
        private final String arconiaPropertyKey;
        private final boolean automaticConversion;

        OtelSdkMetricsProperty(String otelSdkPropertyKey, String arconiaPropertyKey, boolean automaticConversion) {
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
