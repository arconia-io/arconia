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
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.FilterResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.MapResourceContributor;
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
    Resource resource(ObjectProvider<ResourceContributor> resourceContributors) {
        ResourceBuilder builder = Resource.getDefault().toBuilder();
        resourceContributors.orderedStream().forEach(contributor -> contributor.contribute(builder));
        return builder.build();
    }

    @Bean
    @ConditionalOnEnabledResourceContributor(value = "environment", matchIfMissing = true)
    @Order(DEFAULT_ORDER)
    EnvironmentResourceContributor environmentResourceContributor(Environment environment) {
        return new EnvironmentResourceContributor(environment);
    }

    @Bean
    @ConditionalOnBean(BuildProperties.class)
    @ConditionalOnEnabledResourceContributor("build")
    @Order(DEFAULT_ORDER)
    BuildResourceContributor buildResourceContributor(BuildProperties properties) {
        return new BuildResourceContributor(properties);
    }

    @Bean
    @Order(DEFAULT_ORDER + 10)
    MapResourceContributor propertyResourceContributor(OpenTelemetryResourceProperties properties) {
        return new MapResourceContributor(properties.getAttributes());
    }

    @Bean
    @ConditionalOnEnabledResourceContributor(value = "filter", matchIfMissing = true)
    FilterResourceContributor filterResourceContributor(OpenTelemetryResourceProperties properties) {
        return new FilterResourceContributor(properties.getFilter().getDisabledKeys());
    }

}
