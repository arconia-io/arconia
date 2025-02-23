package io.arconia.opentelemetry.autoconfigure.sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

import io.arconia.opentelemetry.autoconfigure.exporter.ExporterType;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;

/**
 * Adapter for OpenTelemetry SDK Autoconfigure properties.
 */
public final class OtelSdkPropertyAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OtelSdkPropertyAdapter.class);

    /**
     * Adapts the OpenTelemetry SDK property to the Arconia property.
     */
    public static void setProperty(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (value != null) {
            arconiaProperties.put(property.getArconiaPropertyKey(), value);
        }
    }

    /**
     * Adapts the OpenTelemetry SDK property the Arconia property as a list.
     */
    public static void setListProperty(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (StringUtils.hasText(value)) {
            List<String> propertyList = new ArrayList<>(List.of(value.split(",")));
            arconiaProperties.put(property.getArconiaPropertyKey(), propertyList);
        }
    }

    /**
     * Adapts the OpenTelemetry SDK property the Arconia property as a map.
     */
    public static void setMapProperty(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (StringUtils.hasText(value)) {
            Map<String,String> propertyMap = new HashMap<>();
            String[] keyValuePairs = value.split(",");
            for (String pair : keyValuePairs) {
                String[] entry = pair.split("=");
                if (entry.length == 2) {
                    propertyMap.put(entry[0], entry[1]);
                } else {
                    logger.warn("Invalid key-value pair in {}: {}", property.getOtelSdkPropertyKey(), pair);
                }
            }
            arconiaProperties.put(property.getArconiaPropertyKey(), propertyMap);
        }
    }

    /**
     * Adapts the OpenTelemetry SDK property for the exporter type to the Arconia property.
     */
    public static void setExporterType(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (StringUtils.hasText(value)) {
            var exporterType = switch (value.toLowerCase().trim()) {
                case "console" -> ExporterType.CONSOLE;
                case "none" -> ExporterType.NONE;
                case "otlp" -> ExporterType.OTLP;
                default -> null;
            };
            if (exporterType == null) {
                logger.warn("Unsupported value for {}: {}", property.getOtelSdkPropertyKey(), value);
            } else {
                arconiaProperties.put(property.getArconiaPropertyKey(), exporterType);
            }
        }
    }

    /**
     * Adapts the OpenTelemetry SDK property for the exporter protocol to the Arconia property.
     */
    public static void setExporterProtocol(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (StringUtils.hasText(value)) {
            var protocol = switch (value.toLowerCase().trim()) {
                case "grpc" -> Protocol.GRPC;
                case "http/protobuf" -> Protocol.HTTP_PROTOBUF;
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
     * Adapts the OpenTelemetry SDK property for the exporter compression to the Arconia property.
     */
    public static void setExporterCompression(OtelSdkProperty property, ConfigurableEnvironment environment, Map<String, Object> arconiaProperties) {
        String value = environment.getProperty(property.getOtelSdkPropertyKey());
        if (StringUtils.hasText(value)) {
            var protocol = switch (value.toLowerCase().trim()) {
                case "gzip" -> Compression.GZIP;
                case "none" -> Compression.NONE;
                default -> null;
            };
            if (protocol == null) {
                logger.warn("Unsupported value for {}: {}", property.getOtelSdkPropertyKey(), value);
            } else {
                arconiaProperties.put(property.getArconiaPropertyKey(), protocol);
            }
        }
    }

}
