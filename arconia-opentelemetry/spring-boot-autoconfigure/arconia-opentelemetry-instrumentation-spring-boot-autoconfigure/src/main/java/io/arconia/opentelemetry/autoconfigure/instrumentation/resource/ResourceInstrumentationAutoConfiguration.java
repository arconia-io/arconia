package io.arconia.opentelemetry.autoconfigure.instrumentation.resource;

import io.opentelemetry.instrumentation.resources.ContainerResource;
import io.opentelemetry.instrumentation.resources.HostIdResource;
import io.opentelemetry.sdk.resources.Resource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

import io.arconia.opentelemetry.autoconfigure.sdk.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.ConditionalOnOpenTelemetryResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.OpenTelemetryResourceAutoConfiguration;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.ResourceContributor;

/**
 * Auto-configuration for OpenTelemetry resource instrumentation.
 */
@AutoConfiguration(before = OpenTelemetryResourceAutoConfiguration.class)
@ConditionalOnClass(Resource.class)
@ConditionalOnOpenTelemetry
public class ResourceInstrumentationAutoConfiguration {

    /**
     * A {@link ResourceContributor} that contributes attributes about the OCI container
     * the application is running in, following the OpenTelemetry Semantic Conventions.
     * <p>
     * The following attributes are populated:
     * <ul>
     *     <li>{@code container.id</li>
     * </ul>
     *
     * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/container">Resource Container Semantic Conventions</a>
     */
    @Bean
    @ConditionalOnOpenTelemetryResourceContributor("container")
    @ConditionalOnClass(ContainerResource.class)
    @Order(OpenTelemetryResourceAutoConfiguration.DEFAULT_ORDER)
    ResourceContributor containerResourceContributor() {
        return builder -> builder.putAll(ContainerResource.get().getAttributes());
    }

    /**
     * A {@link ResourceContributor} that contributes attributes about the host the application is running on,
     * following the OpenTelemetry Semantic Conventions.
     * <p>
     * The following attributes are populated:
     * <ul>
     *     <li>{@code host.id</li>
     * </ul>
     *
     * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/host">Resource Host Semantic Conventions</a>
     */
    @Bean
    @ConditionalOnOpenTelemetryResourceContributor("host-id")
    @ConditionalOnClass(HostIdResource.class)
    @Order(OpenTelemetryResourceAutoConfiguration.DEFAULT_ORDER)
    ResourceContributor hostIdResourceContributor() {
        return builder -> builder.putAll(HostIdResource.get().getAttributes());
    }

}
