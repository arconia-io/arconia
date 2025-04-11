package io.arconia.boot.env;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration for setting profiles based on the application mode.
 */
@AutoConfiguration
@EnableConfigurationProperties(ProfilesProperties.class)
public class ProfilesAutoConfiguration {
}
