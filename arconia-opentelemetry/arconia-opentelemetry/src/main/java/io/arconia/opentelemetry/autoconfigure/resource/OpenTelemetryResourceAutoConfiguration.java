package io.arconia.opentelemetry.autoconfigure.resource;

import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.SchemaUrls;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.BuildResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.EnvironmentResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.HostResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.JavaResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.OsResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.ProcessResourceContributor;
import io.arconia.opentelemetry.autoconfigure.resource.contributor.ResourceContributor;

/**
 * Auto-configuration for OpenTelemetry {@link Resource}.
 */
@AutoConfiguration(after = org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration.class)
@ConditionalOnOpenTelemetry
@EnableConfigurationProperties(OpenTelemetryResourceProperties.class)
public final class OpenTelemetryResourceAutoConfiguration {

    public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Bean
    @ConditionalOnMissingBean
    Resource resource(ObjectProvider<ResourceContributor> resourceContributors,
                      ObjectProvider<OpenTelemetryResourceBuilderCustomizer> customizers
    ) {
        ResourceBuilder builder = Resource.getDefault().toBuilder().setSchemaUrl(SchemaUrls.V1_37_0);
        resourceContributors.orderedStream().forEach(contributor -> contributor.contribute(builder));
        customizers.orderedStream().forEach(customizer -> customizer.customize(builder));
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
    OpenTelemetryResourceBuilderCustomizer filterAttributes(OpenTelemetryResourceProperties properties) {
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
