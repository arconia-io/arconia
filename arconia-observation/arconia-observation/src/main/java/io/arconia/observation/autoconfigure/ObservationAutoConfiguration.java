package io.arconia.observation.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration for observations.
 */
@AutoConfiguration
@EnableConfigurationProperties(ObservationProperties.class)
public class ObservationAutoConfiguration {
}
