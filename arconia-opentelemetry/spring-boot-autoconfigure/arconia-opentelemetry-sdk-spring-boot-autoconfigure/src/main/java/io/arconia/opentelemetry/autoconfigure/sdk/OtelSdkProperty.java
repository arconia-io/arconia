package io.arconia.opentelemetry.autoconfigure.sdk;

/**
 * Provides mapping information between OpenTelemetry SDK Autoconfigure and Arconia properties.
 */
public interface OtelSdkProperty {

    /**
     * The property key for the OpenTelemetry SDK.
     */
    String getOtelSdkPropertyKey();

    /**
     * The property key for the Arconia property.
     */
    String getArconiaPropertyKey();

    /**
     * Whether the property is automatically converted.
     */
    boolean isAutomaticConversion();

}
