package io.arconia.dev.services.core.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Global auto-configuration for Dev Services.
 */
@AutoConfiguration
@EnableConfigurationProperties(DevServicesProperties.class)
public final class DevServicesAutoConfiguration {}
