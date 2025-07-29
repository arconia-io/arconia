package io.arconia.boot.autoconfigure.bootstrap.dev;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for bootstrapping the development mode.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BootstrapDevProperties.class)
public final class BootstrapDevConfiguration {}
