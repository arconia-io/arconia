package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import io.arconia.opentelemetry.autoconfigure.sdk.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.BuildResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.EnvironmentResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.HostResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.JavaResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.OsResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.ProcessResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.ResourceContributor;

/**
 * Auto-configuration for OpenTelemetry {@link Resource}.
 */
@AutoConfiguration
@ConditionalOnOpenTelemetry
@ConditionalOnClass(Resource.class)
@EnableConfigurationProperties(OpenTelemetryResourceProperties.class)
public class OpenTelemetryResourceAutoConfiguration {

    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Bean
    @ConditionalOnMissingBean
    Resource resource(ObjectProvider<ResourceContributor> resourceContributors, ObjectProvider<SdkResourceBuilderCustomizer> customizers) {
        ResourceBuilder builder = Resource.getDefault().toBuilder();
        resourceContributors.orderedStream().forEach(contributor -> contributor.contribute(builder));
        customizers.orderedStream().forEach(contributor -> contributor.customize(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "build", matchIfMissing = true)
    @ConditionalOnBean(BuildProperties.class)
    @Order(DEFAULT_ORDER)
    BuildResourceContributor buildResourceContributor(BuildProperties properties) {
        return new BuildResourceContributor(properties);
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "environment", matchIfMissing = true)
    @Order(Ordered.HIGHEST_PRECEDENCE)
    EnvironmentResourceContributor environmentResourceContributor(Environment environment, OpenTelemetryResourceProperties properties) {
        return new EnvironmentResourceContributor(environment, properties);
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "host")
    @Order(DEFAULT_ORDER)
    HostResourceContributor hostResourceContributor() {
        return new HostResourceContributor();
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "java")
    @Order(DEFAULT_ORDER)
    JavaResourceContributor javaRuntimeResourceContributor() {
        return new JavaResourceContributor();
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "os")
    @Order(DEFAULT_ORDER)
    OsResourceContributor osResourceContributor() {
        return new OsResourceContributor();
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor(value = "process")
    @Order(DEFAULT_ORDER)
    ProcessResourceContributor processRuntimeResourceContributor() {
        return new ProcessResourceContributor();
    }

    @Bean
    SdkResourceBuilderCustomizer filterAttributes(OpenTelemetryResourceProperties properties) {
        return builder -> {
            var attributeKeysMap = properties.getEnable();

            var allKeys = attributeKeysMap.get("all");
            if (allKeys == null) {
                attributeKeysMap.forEach((key, enabled) -> {
                    if (!enabled) {
                        builder.removeIf(attributeKey -> attributeKey.getKey().startsWith(key));
                    }
                });
                return;
            }

            if (!allKeys) {
                builder.removeIf(attributeKey -> true);
            }

        };
    }

}
