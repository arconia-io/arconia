package io.arconia.opentelemetry.autoconfigure.instrumentation.resource;

import io.opentelemetry.instrumentation.resources.ContainerResource;
import io.opentelemetry.instrumentation.resources.HostIdResource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import io.arconia.opentelemetry.autoconfigure.sdk.resource.ConditionalOnOpenTelemetryResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.OpenTelemetryResourceAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.ResourceContributor;

/**
 * Auto-configuration for OpenTelemetry resource instrumentation.
 */
@AutoConfiguration(before = OpenTelemetryResourceAutoConfiguration.class)
public class ResourceInstrumentationAutoConfiguration {

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor("container")
    @ConditionalOnClass(ContainerResource.class)
    @Order(OpenTelemetryResourceAutoConfiguration.DEFAULT_ORDER)
    ResourceContributor containerResourceContributor() {
        return builder -> builder.putAll(ContainerResource.get().getAttributes());
    }

    @Bean
    @ConditionalOnOpenTelemetryResourceContributor("host-id")
    @ConditionalOnClass(HostIdResource.class)
    @Order(OpenTelemetryResourceAutoConfiguration.DEFAULT_ORDER)
    ResourceContributor hostIdResourceContributor() {
        return builder -> builder.putAll(HostIdResource.get().getAttributes());
    }

}
