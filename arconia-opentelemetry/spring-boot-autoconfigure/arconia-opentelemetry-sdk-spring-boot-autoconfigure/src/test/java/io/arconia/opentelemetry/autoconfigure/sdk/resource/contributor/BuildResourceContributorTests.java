package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import java.util.Properties;

import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ServiceAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BuildResourceContributor}.
 */
class BuildResourceContributorTests {

    private final ResourceBuilder resourceBuilder = mock(ResourceBuilder.class);

    @Test
    void shouldAddVersionWhenPresent() {
        Properties properties = new Properties();
        properties.setProperty("version", "1.0.0");
        BuildProperties buildProperties = new BuildProperties(properties);
        BuildResourceContributor contributor = new BuildResourceContributor(buildProperties);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ServiceAttributes.SERVICE_VERSION, "1.0.0");
    }

    @Test
    void shouldNotAddVersionWhenEmpty() {
        Properties properties = new Properties();
        properties.setProperty("version", "");
        BuildProperties buildProperties = new BuildProperties(properties);
        BuildResourceContributor contributor = new BuildResourceContributor(buildProperties);

        contributor.contribute(resourceBuilder);

        verifyNoInteractions(resourceBuilder);
    }

    @Test
    void shouldNotAddVersionWhenBlank() {
        Properties properties = new Properties();
        properties.setProperty("version", "   ");
        BuildProperties buildProperties = new BuildProperties(properties);
        BuildResourceContributor contributor = new BuildResourceContributor(buildProperties);

        contributor.contribute(resourceBuilder);

        verifyNoInteractions(resourceBuilder);
    }

    @Test
    void shouldNotAddVersionWhenNull() {
        BuildProperties buildProperties = mock(BuildProperties.class);
        when(buildProperties.getVersion()).thenReturn(null);
        BuildResourceContributor contributor = new BuildResourceContributor(buildProperties);

        contributor.contribute(resourceBuilder);

        verifyNoInteractions(resourceBuilder);
    }

}
