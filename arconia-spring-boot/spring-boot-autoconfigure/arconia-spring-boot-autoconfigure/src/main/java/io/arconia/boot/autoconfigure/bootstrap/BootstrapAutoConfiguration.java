package io.arconia.boot.autoconfigure.bootstrap;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import io.arconia.boot.autoconfigure.bootstrap.dev.BootstrapDevConfiguration;
import io.arconia.boot.autoconfigure.bootstrap.test.BootstrapTestConfiguration;

/**
 * Auto-configuration for bootstrapping an Arconia-flavored Spring Boot application.
 */
@AutoConfiguration
@Import({
    BootstrapDevConfiguration.class, 
    BootstrapTestConfiguration.class
})
@EnableConfigurationProperties(BootstrapProperties.class)
public class BootstrapAutoConfiguration {
}
