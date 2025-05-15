package io.arconia.dev.services.oracle;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.oracle.OracleDevServicesAutoConfiguration.ConfigurationWithRestart;
import io.arconia.dev.services.oracle.OracleDevServicesAutoConfiguration.ConfigurationWithoutRestart;

/**
 * Auto-configuration for Oracle Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnProperty(prefix = OracleDevServicesProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OracleDevServicesProperties.class)
@Import({ConfigurationWithRestart.class, ConfigurationWithoutRestart.class})
public class OracleDevServicesAutoConfiguration {

    public static final String COMPATIBLE_IMAGE_NAME = "gvenzl/oracle-free";

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(RestartScope.class)
    public static class ConfigurationWithRestart {

        @Bean
        @RestartScope
        @ServiceConnection
        @ConditionalOnMissingBean
        OracleContainer oracleContainer(OracleDevServicesProperties properties, ApplicationContext applicationContext) {
            return new OracleContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withEnv(properties.getEnvironment())
                    .withReuse(properties.getShared().asBoolean(applicationContext));
        }

    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.boot.devtools.restart.RestartScope")
    public static class ConfigurationWithoutRestart {

        @Bean
        @ServiceConnection
        @ConditionalOnMissingBean
        OracleContainer oracleContainerNoRestartScope(OracleDevServicesProperties properties, ApplicationContext applicationContext) {
            return new OracleContainer(DockerImageName.parse(properties.getImageName())
                    .asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME))
                    .withStartupTimeout(properties.getStartupTimeout())
                    .withEnv(properties.getEnvironment())
                    .withReuse(properties.getShared().asBoolean(applicationContext));
        }

    }

}
