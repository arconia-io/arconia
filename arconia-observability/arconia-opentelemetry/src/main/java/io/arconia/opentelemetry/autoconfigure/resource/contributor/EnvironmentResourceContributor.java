package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ServiceAttributes;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;
import io.arconia.opentelemetry.autoconfigure.resource.OpenTelemetryResourceProperties;

/**
 * A {@link ResourceContributor} that contributes attributes from the Spring environment and configuration properties,
 * following the OpenTelemetry Semantic Conventions.
 * <p>
 * The following attributes are populated:
 * <ul>
 *     <li>{@code service.name}</li>
 *     <li>{@code service.namespace}</li>
 *     <li>{@code service.instance.id}</li>
 *     <li>{@code webengine.name}</li>
 *     <li>{@code webengine.version}</li>
 * </ul>
 * <p>
 * Furthermore, any additional attributes defined in the {@link OpenTelemetryResourceProperties} are also populated.
 *
 * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/#service">Resource Service Semantic Conventions</a>
 * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/webengine">Resource WebEngine Semantic Conventions</a>
 */
@Incubating
public final class EnvironmentResourceContributor implements ResourceContributor {

    // These semantic conventions are experimental, so we define them explicitly to be able to ensure backward
    // compatibility rather than using the constants from OpenTelemetry SemConv project that may change in the future
    // without considering backward compatibility.
    public static final AttributeKey<String> SERVICE_INSTANCE_ID = AttributeKey.stringKey("service.instance.id");
    public static final AttributeKey<String> SERVICE_NAMESPACE = AttributeKey.stringKey("service.namespace");
    public static final AttributeKey<String> WEBENGINE_NAME = AttributeKey.stringKey("webengine.name");
    public static final AttributeKey<String> WEBENGINE_VERSION = AttributeKey.stringKey("webengine.version");

    private static final String DEFAULT_SERVICE_INSTANCE_ID = UUID.randomUUID().toString();
    private static final String DEFAULT_SERVICE_NAME = "unknown_service:java";
    private static final String SPRING_BOOT_NAME = "Spring Boot";

    private final Environment environment;
    private final OpenTelemetryResourceProperties properties;

    public EnvironmentResourceContributor(Environment environment, OpenTelemetryResourceProperties properties) {
        this.environment = environment;
        this.properties = properties;
    }

    @Override
    public void contribute(ResourceBuilder builder) {
        builder.putAll(computeAttributes());

        builder.put(ServiceAttributes.SERVICE_NAME, computeServiceName());

        String serviceNamespace = computeServiceNamespace();
        if (StringUtils.hasText(serviceNamespace)) {
            builder.put(SERVICE_NAMESPACE, serviceNamespace);
        }

        String serviceInstanceId = computeServiceInstanceId();
        builder.put(SERVICE_INSTANCE_ID, serviceInstanceId);

        builder.put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        builder.put(WEBENGINE_VERSION, SpringBootVersion.getVersion());
    }

    /**
     * Compute the attributes from the {@link OpenTelemetryResourceProperties}.
     * Values are URL-decoded since they are expected to be URL-encoded.
     *
     * @link <a href="https://opentelemetry.io/docs/specs/otel/resource/sdk/#specifying-resource-information-via-an-environment-variable">Resource Attributes</a>
     */
    private Attributes computeAttributes() {
        Map<AttributeKey<String>, String> attributesMap = properties.getAttributes().entrySet().stream()
                .map(e -> Map.entry(e.getKey(), URLDecoder.decode(e.getValue(), StandardCharsets.UTF_8)))
                .collect(Collectors.toMap(e -> AttributeKey.stringKey(e.getKey()), Map.Entry::getValue));

        AttributesBuilder attributesBuilder = Attributes.builder();
        attributesMap.forEach(attributesBuilder::put);
        return attributesBuilder.build();
    }

    private String computeServiceName() {
        String serviceName = properties.getServiceName();
        if (!StringUtils.hasText(serviceName)) {
            serviceName = properties.getAttributes().get(ServiceAttributes.SERVICE_NAME.getKey());
        }
        if (!StringUtils.hasText(serviceName)) {
            serviceName = environment.getProperty("spring.application.name", DEFAULT_SERVICE_NAME);
        }
        return serviceName;
    }

    @Nullable
    private String computeServiceNamespace() {
        String serviceNamespace = properties.getAttributes().get(SERVICE_NAMESPACE.getKey());
        if (!StringUtils.hasText(serviceNamespace)) {
            serviceNamespace = environment.getProperty("spring.application.group");
        }
        return serviceNamespace;
    }

    private String computeServiceInstanceId() {
        String serviceInstanceId = properties.getAttributes().get(SERVICE_INSTANCE_ID.getKey());
        if (!StringUtils.hasText(serviceInstanceId)) {
            serviceInstanceId = DEFAULT_SERVICE_INSTANCE_ID;
        }
        return serviceInstanceId;
    }

}
