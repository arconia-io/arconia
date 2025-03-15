package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ServiceAttributes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.SpringBootVersion;
import org.springframework.mock.env.MockEnvironment;

import io.arconia.opentelemetry.autoconfigure.sdk.resource.OpenTelemetryResourceProperties;

import static io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.EnvironmentResourceContributor.SERVICE_INSTANCE_ID;
import static io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.EnvironmentResourceContributor.SERVICE_NAMESPACE;
import static io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.EnvironmentResourceContributor.WEBENGINE_NAME;
import static io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.EnvironmentResourceContributor.WEBENGINE_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link EnvironmentResourceContributor}.
 */
class EnvironmentResourceContributorTests {

    private static final String DEFAULT_SERVICE_NAME = "unknown_service:java";
    private static final String SPRING_BOOT_NAME = "Spring Boot";

    private ResourceBuilder resourceBuilder;
    private MockEnvironment environment;
    private OpenTelemetryResourceProperties properties;

    @BeforeEach
    void setUp() {
        resourceBuilder = mock(ResourceBuilder.class);
        environment = new MockEnvironment();
        properties = new OpenTelemetryResourceProperties();
    }

    @Test
    void shouldUseDefaultServiceNameWhenPropertyNotSet() {
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());
        verify(resourceBuilder).putAll(Attributes.empty());
    }

    @Test
    void shouldUseCustomServiceNameWhenPropertyIsSet() {
        environment.setProperty("spring.application.name", "test-service");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, "test-service");
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());
        verify(resourceBuilder).putAll(Attributes.empty());
    }

    @Test
    void shouldAddServiceNamespaceWhenPropertyIsSet() {
        environment.setProperty("spring.application.group", "test-group");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verify(resourceBuilder).put(SERVICE_NAMESPACE, "test-group");
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());
        verify(resourceBuilder).putAll(Attributes.empty());
    }

    @Test
    void shouldNotAddServiceNamespaceWhenPropertyIsEmpty() {
        environment.setProperty("spring.application.group", "");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());
        verify(resourceBuilder).putAll(Attributes.empty());
    }

    @Test
    void shouldNotAddServiceNamespaceWhenPropertyIsBlank() {
        environment.setProperty("spring.application.group", "   ");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());
        verify(resourceBuilder).putAll(Attributes.empty());
    }

    @Test
    void shouldNotAddServiceNamespaceWhenPropertyIsNotSet() {
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());
        verify(resourceBuilder).putAll(Attributes.empty());
    }

    @Test
    void shouldAddBothServiceNameAndNamespaceWhenPropertiesAreSet() {
        environment.setProperty("spring.application.name", "test-service");
        environment.setProperty("spring.application.group", "test-group");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, "test-service");
        verify(resourceBuilder).put(SERVICE_NAMESPACE, "test-group");
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());
        verify(resourceBuilder).putAll(Attributes.empty());
    }

    @Test
    void shouldUseServiceNameFromPropertiesWhenSet() {
        properties.setServiceName("property-service");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, "property-service");
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());
        verify(resourceBuilder).putAll(Attributes.empty());
    }

    @Test
    void shouldUseServiceNameFromAttributesWhenSet() {
        properties.getAttributes().put(ServiceAttributes.SERVICE_NAME.getKey(), "attribute-service");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, "attribute-service");
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);
        verify(resourceBuilder).putAll(attributesCaptor.capture());
        assertThat(attributesCaptor.getValue().get(ServiceAttributes.SERVICE_NAME)).isEqualTo("attribute-service");
    }

    @Test
    void shouldPreferServiceNamePropertyOverAttributeWhenBothSet() {
        properties.setServiceName("property-service");
        properties.getAttributes().put(ServiceAttributes.SERVICE_NAME.getKey(), "attribute-service");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, "property-service");
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);
        verify(resourceBuilder).putAll(attributesCaptor.capture());
        assertThat(attributesCaptor.getValue().get(ServiceAttributes.SERVICE_NAME)).isEqualTo("attribute-service");
    }

    @Test
    void shouldUseServiceNamespaceFromAttributesWhenSet() {
        properties.getAttributes().put(SERVICE_NAMESPACE.getKey(), "attribute-namespace");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verify(resourceBuilder).put(SERVICE_NAMESPACE, "attribute-namespace");
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), anyString());
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);
        verify(resourceBuilder).putAll(attributesCaptor.capture());
        assertThat(attributesCaptor.getValue().get(SERVICE_NAMESPACE)).isEqualTo("attribute-namespace");
    }

    @Test
    void shouldUseServiceInstanceIdFromAttributesWhenSet() {
        properties.getAttributes().put(SERVICE_INSTANCE_ID.getKey(), "custom-instance-id");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verify(resourceBuilder).put(eq(SERVICE_INSTANCE_ID), eq("custom-instance-id"));
        verify(resourceBuilder).put(WEBENGINE_NAME, SPRING_BOOT_NAME);
        verify(resourceBuilder).put(WEBENGINE_VERSION, SpringBootVersion.getVersion());

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);
        verify(resourceBuilder).putAll(attributesCaptor.capture());
        assertThat(attributesCaptor.getValue().get(SERVICE_INSTANCE_ID)).isEqualTo("custom-instance-id");
    }

    @Test
    void shouldDecodeUrlEncodedAttributeValues() {
        properties.getAttributes().put("test.key", "encoded%20value");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);
        verify(resourceBuilder).putAll(attributesCaptor.capture());
        assertThat(attributesCaptor.getValue().get(AttributeKey.stringKey("test.key"))).isEqualTo("encoded value");
    }

    @Test
    void shouldAddCustomAttributesFromProperties() {
        properties.getAttributes().put("custom.key1", "value1");
        properties.getAttributes().put("custom.key2", "value2");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment, properties);

        contributor.contribute(resourceBuilder);

        ArgumentCaptor<Attributes> attributesCaptor = ArgumentCaptor.forClass(Attributes.class);
        verify(resourceBuilder).putAll(attributesCaptor.capture());
        assertThat(attributesCaptor.getValue().get(AttributeKey.stringKey("custom.key1"))).isEqualTo("value1");
        assertThat(attributesCaptor.getValue().get(AttributeKey.stringKey("custom.key2"))).isEqualTo("value2");
    }

}
