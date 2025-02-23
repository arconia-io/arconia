package io.arconia.opentelemetry.autoconfigure.instrumentation.micrometer;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = MicrometerProperties.CONFIG_PREFIX)
public class MicrometerProperties {

    public static final String INSTRUMENTATION_NAME = "micrometer";

    public static final String CONFIG_PREFIX = "arconia.otel.instrumentation." + INSTRUMENTATION_NAME;

}
