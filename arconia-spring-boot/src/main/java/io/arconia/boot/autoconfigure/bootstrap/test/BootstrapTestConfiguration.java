package io.arconia.boot.autoconfigure.bootstrap.test;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for bootstrapping the test mode.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(BootstrapTestProperties.class)
public final class BootstrapTestConfiguration {}
