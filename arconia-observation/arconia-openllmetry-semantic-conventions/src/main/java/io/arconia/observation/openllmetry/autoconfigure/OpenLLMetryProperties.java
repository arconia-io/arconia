package io.arconia.observation.openllmetry.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.observation.openllmetry.instrumentation.OpenLLMetryOptions;

/**
 * Configuration properties for the OpenLLMetry instrumentation.
 */
@ConfigurationProperties(prefix = OpenLLMetryProperties.CONFIG_PREFIX)
public class OpenLLMetryProperties extends OpenLLMetryOptions {

    public static final String CONFIG_PREFIX = "arconia.observations.conventions.openllmetry";

}
