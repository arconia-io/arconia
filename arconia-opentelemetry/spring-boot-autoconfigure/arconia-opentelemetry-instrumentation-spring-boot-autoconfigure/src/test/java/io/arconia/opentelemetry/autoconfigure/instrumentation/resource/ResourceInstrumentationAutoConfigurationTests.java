package io.arconia.opentelemetry.autoconfigure.instrumentation.resource;

import io.opentelemetry.instrumentation.resources.ContainerResource;
import io.opentelemetry.instrumentation.resources.HostIdResource;
import io.opentelemetry.sdk.resources.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.ResourceContributor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ResourceInstrumentationAutoConfiguration}.
 */
class ResourceInstrumentationAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResourceInstrumentationAutoConfiguration.class))
            .withPropertyValues("arconia.opentelemetry.enabled=true");

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner.withPropertyValues("arconia.opentelemetry.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ResourceContributor.class);
                    assertThat(context).doesNotHaveBean("containerResourceContributor");
                    assertThat(context).doesNotHaveBean("hostIdResourceContributor");
                });
    }

    @Test
    void autoConfigurationNotActivatedWhenResourceClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(Resource.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ResourceContributor.class);
                    assertThat(context).doesNotHaveBean("containerResourceContributor");
                    assertThat(context).doesNotHaveBean("hostIdResourceContributor");
                });
    }

    @Test
    void containerResourceContributorNotCreatedWhenContainerResourceClassMissing() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.container.enabled=true")
                .withClassLoader(new FilteredClassLoader(ContainerResource.class))
                .run(context -> assertThat(context).doesNotHaveBean("containerResourceContributor"));
    }

    @Test
    void containerResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.container.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("containerResourceContributor"));
    }

    @Test
    void containerResourceContributorCreatedWhenEnabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.container.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourceContributor.class);
                    assertThat(context.getBean("containerResourceContributor")).isInstanceOf(ResourceContributor.class);
                });
    }

    @Test
    void hostIdResourceContributorNotCreatedWhenHostIdResourceClassMissing() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.host-id.enabled=true")
                .withClassLoader(new FilteredClassLoader(HostIdResource.class))
                .run(context -> assertThat(context).doesNotHaveBean("hostIdResourceContributor"));
    }

    @Test
    void hostIdResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.host-id.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("hostIdResourceContributor"));
    }

    @Test
    void hostIdResourceContributorCreatedWhenEnabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.host-id.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(ResourceContributor.class);
                    assertThat(context.getBean("hostIdResourceContributor")).isInstanceOf(ResourceContributor.class);
                });
    }

    @Test
    void bothResourceContributorsCreatedWhenEnabled() {
        contextRunner
                .withPropertyValues(
                    "arconia.otel.resource.contributors.container.enabled=true",
                    "arconia.otel.resource.contributors.host-id.enabled=true"
                )
                .run(context -> {
                    assertThat(context).getBeans(ResourceContributor.class).hasSize(2);
                    assertThat(context.getBean("containerResourceContributor")).isInstanceOf(ResourceContributor.class);
                    assertThat(context.getBean("hostIdResourceContributor")).isInstanceOf(ResourceContributor.class);
                });
    }

}
