package io.arconia.observation.openinference.ai.instrumentation;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceResourceContributor}.
 */
class OpenInferenceResourceContributorTests {

    private static final AttributeKey<String> PROJECT_NAME = AttributeKey.stringKey("openinference.project.name");

    @Test
    void shouldContributeConfiguredProjectName() {
        ResourceBuilder builder = Resource.builder().put("service.name", "my-service");

        new OpenInferenceResourceContributor("my-project").contribute(builder);

        assertThat(builder.build().getAttribute(PROJECT_NAME)).isEqualTo("my-project");
    }

    @Test
    void shouldFallBackToServiceName() {
        ResourceBuilder builder = Resource.builder().put("service.name", "my-service");

        new OpenInferenceResourceContributor(null).contribute(builder);

        assertThat(builder.build().getAttribute(PROJECT_NAME)).isEqualTo("my-service");
    }

    @Test
    void shouldContributeNothingWithoutProjectNameNorServiceName() {
        ResourceBuilder builder = Resource.builder();

        new OpenInferenceResourceContributor(" ").contribute(builder);

        assertThat(builder.build().getAttribute(PROJECT_NAME)).isNull();
    }

}
