package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ServiceAttributes;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * A {@link ResourceContributor} that contributes attributes from the Spring {@link Environment}.
 */
public class EnvironmentResourceContributor implements ResourceContributor {

    // Used in Spring Boot, but not found in OpenTelemetry (which uses "service.namespace" for the same purpose).
    private static final AttributeKey<String> SERVICE_GROUP = AttributeKey.stringKey("service.group");
    private static final String DEFAULT_SERVICE_NAME = "unknown_service:java";

    private final Environment environment;

    public EnvironmentResourceContributor(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void contribute(ResourceBuilder builder) {
        String serviceName = environment.getProperty("spring.application.name", DEFAULT_SERVICE_NAME);
        builder.put(ServiceAttributes.SERVICE_NAME, serviceName);

        String serviceGroup = environment.getProperty("spring.application.group");
        if (StringUtils.hasText(serviceGroup)) {
            builder.put(SERVICE_GROUP, serviceGroup);
        }
    }

}
