package io.arconia.opentelemetry.autoconfigure.tracing.sdk;

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

import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkProperty;
import io.arconia.opentelemetry.autoconfigure.sdk.OtelSdkPropertyAdapter;
import io.arconia.opentelemetry.autoconfigure.tracing.OpenTelemetryTracingProperties;

/**
 * Converts OpenTelemetry SDK Autoconfigure properties to Arconia properties.
 */
public class OpenTelemetryTracingEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(OpenTelemetryTracingEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> arconiaProperties = new HashMap<>();
        Stream.of(OtelSdkTracingProperty.values())
                .filter(OtelSdkTracingProperty::isAutomaticConversion)
                .forEach(property -> OtelSdkPropertyAdapter.setProperty(property, environment, arconiaProperties));

        setTraceSampler(OtelSdkTracingProperty.TRACER_SAMPLER, environment, arconiaProperties);
        setTraceSamplerArg(OtelSdkTracingProperty.TRACER_SAMPLER_ARG, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setMapProperty(OtelSdkTracingProperty.EXPORTER_OTLP_TRACES_HEADERS, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterType(OtelSdkTracingProperty.TRACES_EXPORTER, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterProtocol(OtelSdkTracingProperty.EXPORTER_OTLP_TRACES_PROTOCOL, environment, arconiaProperties);
        OtelSdkPropertyAdapter.setExporterCompression(OtelSdkTracingProperty.EXPORTER_OTLP_TRACES_COMPRESSION, environment, arconiaProperties);

        MapPropertySource mapPropertySource = new MapPropertySource("Arconia OpenTelemetry Tracing", arconiaProperties);
        MutablePropertySources mutablePropertySources = environment.getPropertySources();
        mutablePropertySources.addFirst(mapPropertySource);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    /**
     * Adapts the OpenTelemetry SDK property for the trace sampler to the Arconia property.
     */
    static void setTraceSampler(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (StringUtils.hasText(value)) {
            var protocol = switch (value.toLowerCase().trim()) {
                case "always_on" -> OpenTelemetryTracingProperties.SamplingStrategy.ALWAYS_ON;
                case "always_off" -> OpenTelemetryTracingProperties.SamplingStrategy.ALWAYS_OFF;
                case "traceidratio" -> OpenTelemetryTracingProperties.SamplingStrategy.TRACE_ID_RATIO;
                case "parentbased_always_on" -> OpenTelemetryTracingProperties.SamplingStrategy.PARENT_BASED_ALWAYS_ON;
                case "parentbased_always_off" -> OpenTelemetryTracingProperties.SamplingStrategy.PARENT_BASED_ALWAYS_OFF;
                case "parentbased_traceidratio" -> OpenTelemetryTracingProperties.SamplingStrategy.PARENT_BASED_TRACE_ID_RATIO;
                default -> null;
            };
            if (protocol == null) {
                logger.warn("Unsupported value for {}: {}", property.getOtelSdkPropertyKey(), value);
            } else {
                arconiaProperties.put(property.getArconiaPropertyKey(), protocol);
            }
        }
    }

    /**
     * Adapts the OpenTelemetry SDK property for the trace sampler arg to the Arconia property.
     */
    static void setTraceSamplerArg(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());

        if (StringUtils.hasText(value)) {
            try {
                arconiaProperties.put(property.getArconiaPropertyKey(), Double.valueOf(value.trim()));
            } catch (NumberFormatException ex) {
                logger.warn("Unsupported value for {}: {}", property.getOtelSdkPropertyKey(), value);
            }
        }
    }

    enum OtelSdkTracingProperty implements OtelSdkProperty {

        BSP_SCHEDULE_DELAY("otel.bsp.schedule.delay", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.scheduleDelay", true),
        BSP_MAX_QUEUE_SIZE("otel.bsp.max.queue.size", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.maxQueueSize", true),
        BSP_MAX_EXPORT_BATCH_SIZE("otel.bsp.max.export.batch.size", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.maxExportBatchSize", true),
        BSP_EXPORT_TIMEOUT("otel.bsp.export.timeout", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".processor.exporterTimeout", true),

        ATTRIBUTE_VALUE_LENGTH_LIMIT("otel.attribute.value.length.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".spanLimits.maxAttributeValueLength", true),
        ATTRIBUTE_COUNT_LIMIT("otel.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".spanLimits.maxNumberOfAttributes", true),

        SPAN_ATTRIBUTE_VALUE_LENGTH_LIMIT("otel.span.attribute.value.length.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".spanLimits.maxAttributeValueLength", true),
        SPAN_ATTRIBUTE_COUNT_LIMIT("otel.span.attribute.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".spanLimits.maxNumberOfAttributes", true),
        SPAN_EVENT_COUNT_LIMIT("otel.span.event.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".spanLimits.maxNumberOfEvents", true),
        SPAN_LINK_COUNT_LIMIT("otel.span.link.count.limit", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".spanLimits.maxNumberOfLinks", true),

        TRACER_SAMPLER("otel.tracer.sampler", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.strategy", false),
        TRACER_SAMPLER_ARG("otel.tracer.sampler.arg", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".sampling.probability", false),

        TRACES_EXPORTER("otel.traces.exporter", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".type", false),
        EXPORTER_OTLP_TRACES_PROTOCOL("otel.exporter.otlp.traces.protocol", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".otlp.protocol", false),
        EXPORTER_OTLP_TRACES_ENDPOINT("otel.exporter.otlp.traces.endpoint", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".otlp.endpoint", true),
        EXPORTER_OTLP_TRACES_HEADERS("otel.exporter.otlp.traces.headers", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".otlp.headers", false),
        EXPORTER_OTLP_TRACES_COMPRESSION("otel.exporter.otlp.traces.compression", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".otlp.compression", false),
        EXPORTER_OTLP_TRACES_TIMEOUT("otel.exporter.otlp.traces.timeout", OpenTelemetryTracingProperties.CONFIG_PREFIX + ".otlp.timeout", true);

        private final String otelSdkPropertyKey;
        private final String arconiaPropertyKey;
        private final boolean automaticConversion;

        OtelSdkTracingProperty(String otelSdkPropertyKey, String arconiaPropertyKey, boolean automaticConversion) {
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
