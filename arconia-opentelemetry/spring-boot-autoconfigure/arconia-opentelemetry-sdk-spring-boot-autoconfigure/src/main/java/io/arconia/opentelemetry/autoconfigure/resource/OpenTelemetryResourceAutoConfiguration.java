package io.arconia.opentelemetry.autoconfigure.resource;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ServiceAttributes;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Auto-configuration for OpenTelemetry {@link Resource}.
 */
@AutoConfiguration
@EnableConfigurationProperties(OpenTelemetryResourceProperties.class)
public class OpenTelemetryResourceAutoConfiguration {

    // Used in Spring Boot, but not found in OpenTelemetry (which uses "service.namespace" for the same purpose).
    private static final AttributeKey<String> SERVICE_GROUP = AttributeKey.stringKey("service.group");
    private static final String DEFAULT_SERVICE_NAME = "unknown_service:java";

    @Bean
    @ConditionalOnMissingBean
    Resource openTelemetryResource(ObjectProvider<SdkResourceBuilderCustomizer> customizers) {
        ResourceBuilder builder = Resource.getDefault().toBuilder();
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = OpenTelemetryResourceProperties.CONFIG_PREFIX + ".providers", name = "environment", havingValue = "true", matchIfMissing = true)
    SdkResourceBuilderCustomizer environmentResourceCustomizer(Environment environment) {
        return builder -> {
            String serviceName = environment.getProperty("spring.application.name", DEFAULT_SERVICE_NAME);
            String serviceGroup = environment.getProperty("spring.application.group");
            builder.put(ServiceAttributes.SERVICE_NAME, serviceName);
            if (StringUtils.hasText(serviceGroup)) {
                builder.put(SERVICE_GROUP, serviceGroup);
            }
        };
    }

    @Bean
    @ConditionalOnBean(BuildProperties.class)
    @ConditionalOnProperty(prefix = OpenTelemetryResourceProperties.CONFIG_PREFIX + ".providers", name = "build", havingValue = "true", matchIfMissing = true)
    SdkResourceBuilderCustomizer buildResourceCustomizer(BuildProperties buildProperties) {
        return builder -> {
            String version = buildProperties.getVersion();
            if (StringUtils.hasLength(version)) {
                builder.put(ServiceAttributes.SERVICE_VERSION, version);
            }
        };
    }

    @Bean
    SdkResourceBuilderCustomizer propertiesResourceCustomizer(OpenTelemetryResourceProperties properties) {
        return builder -> properties.getAttributes().forEach(builder::put);
    }

    @Bean
    SdkResourceBuilderCustomizer disableKeysResourceCustomizer(OpenTelemetryResourceProperties properties) {
        return builder -> builder.removeIf(key -> properties.getDisabledKeys().contains(key.getKey()));
    }

}
