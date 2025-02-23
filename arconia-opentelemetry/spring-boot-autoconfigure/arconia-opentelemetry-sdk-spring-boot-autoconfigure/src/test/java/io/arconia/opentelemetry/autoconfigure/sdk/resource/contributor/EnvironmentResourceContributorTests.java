package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ServiceAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for {@link EnvironmentResourceContributor}.
 */
class EnvironmentResourceContributorTests {

    private static final AttributeKey<String> SERVICE_GROUP = AttributeKey.stringKey("service.group");
    private static final String DEFAULT_SERVICE_NAME = "unknown_service:java";

    private final ResourceBuilder resourceBuilder = mock(ResourceBuilder.class);
    private final MockEnvironment environment = new MockEnvironment();

    @Test
    void shouldUseDefaultServiceNameWhenPropertyNotSet() {
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verifyNoMoreInteractions(resourceBuilder);
    }

    @Test
    void shouldUseCustomServiceNameWhenPropertyIsSet() {
        environment.setProperty("spring.application.name", "test-service");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, "test-service");
        verifyNoMoreInteractions(resourceBuilder);
    }

    @Test
    void shouldAddServiceGroupWhenPropertyIsSet() {
        environment.setProperty("spring.application.group", "test-group");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verify(resourceBuilder).put(SERVICE_GROUP, "test-group");
        verifyNoMoreInteractions(resourceBuilder);
    }

    @Test
    void shouldNotAddServiceGroupWhenPropertyIsEmpty() {
        environment.setProperty("spring.application.group", "");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verifyNoMoreInteractions(resourceBuilder);
    }

    @Test
    void shouldNotAddServiceGroupWhenPropertyIsBlank() {
        environment.setProperty("spring.application.group", "   ");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verifyNoMoreInteractions(resourceBuilder);
    }

    @Test
    void shouldNotAddServiceGroupWhenPropertyIsNotSet() {
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, DEFAULT_SERVICE_NAME);
        verifyNoMoreInteractions(resourceBuilder);
    }

    @Test
    void shouldAddBothServiceNameAndGroupWhenPropertiesAreSet() {
        environment.setProperty("spring.application.name", "test-service");
        environment.setProperty("spring.application.group", "test-group");
        EnvironmentResourceContributor contributor = new EnvironmentResourceContributor(environment);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_NAME, "test-service");
        verify(resourceBuilder).put(SERVICE_GROUP, "test-group");
        verifyNoMoreInteractions(resourceBuilder);
    }

}
