package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import java.util.Properties;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.BuildResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.EnvironmentResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.HostResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.JavaResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.OsResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.ProcessResourceContributor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryResourceAutoConfiguration}.
 */
class OpenTelemetryResourceAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryResourceAutoConfiguration.class))
            .withPropertyValues("arconia.otel.enabled=true");

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.otel.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(Resource.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenSdkNotPresent() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(Resource.class))
                .run(context -> assertThat(context).doesNotHaveBean(Resource.class));
    }

    @Test
    void autoConfigurationNotActivatedWhenResourceClassMissing() {
        contextRunner.withClassLoader(new FilteredClassLoader(Resource.class))
                .run(context -> assertThat(context).doesNotHaveBean(Resource.class));
    }

    @Test
    void resourceAvailableWithDefaultConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Resource.class);
            assertThat(context).doesNotHaveBean(BuildResourceContributor.class);
            assertThat(context).hasSingleBean(EnvironmentResourceContributor.class);
            assertThat(context).hasSingleBean(OpenTelemetryResourceBuilderCustomizer.class);
            assertThat(context).doesNotHaveBean(HostResourceContributor.class);
            assertThat(context).doesNotHaveBean(JavaResourceContributor.class);
            assertThat(context).doesNotHaveBean(OsResourceContributor.class);
            assertThat(context).doesNotHaveBean(ProcessResourceContributor.class);
        });
    }

    @Test
    void buildResourceContributorCreatedWhenEnabledAndBuildPropertiesAvailable() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.build.enabled=true")
                .withUserConfiguration(BuildPropertiesConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).hasSingleBean(BuildResourceContributor.class);
                });
    }

    @Test
    void buildResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.build.enabled=false")
                .withUserConfiguration(BuildPropertiesConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(BuildResourceContributor.class);
                });
    }

    @Test
    void buildResourceContributorNotCreatedWhenBuildPropertiesNotAvailable() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.build.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(BuildResourceContributor.class);
                });
    }

    @Test
    void environmentResourceContributorCreatedWhenEnabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.environment.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).hasSingleBean(EnvironmentResourceContributor.class);
                });
    }

    @Test
    void environmentResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.environment.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(EnvironmentResourceContributor.class);
                });
    }

    @Test
    void hostResourceContributorCreatedWhenEnabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.host.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).hasSingleBean(HostResourceContributor.class);
                });
    }

    @Test
    void hostResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.host.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(HostResourceContributor.class);
                });
    }

    @Test
    void javaResourceContributorCreatedWhenEnabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.java.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).hasSingleBean(JavaResourceContributor.class);
                });
    }

    @Test
    void javaResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.java.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(JavaResourceContributor.class);
                });
    }

    @Test
    void osResourceContributorCreatedWhenEnabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.os.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).hasSingleBean(OsResourceContributor.class);
                });
    }

    @Test
    void osResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.os.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(OsResourceContributor.class);
                });
    }

    @Test
    void processResourceContributorCreatedWhenEnabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.process.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).hasSingleBean(ProcessResourceContributor.class);
                });
    }

    @Test
    void processResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.otel.resource.contributors.process.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(ProcessResourceContributor.class);
                });
    }

    @Test
    void customResourceTakesPrecedence() {
        contextRunner.withUserConfiguration(CustomResourceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context.getBean(Resource.class))
                            .isSameAs(context.getBean(CustomResourceConfiguration.class).customResource());
                });
    }

    @Test
    void attributeFilteringAppliedWhenSpecificAttributesDisabled() {
        contextRunner.withPropertyValues(
                "arconia.otel.resource.enable.host=false",
                "arconia.otel.resource.enable.process.pid=false"
        ).run(context -> {
            assertThat(context).hasSingleBean(Resource.class);
            assertThat(context).hasSingleBean(OpenTelemetryResourceBuilderCustomizer.class);

            OpenTelemetryResourceBuilderCustomizer customizer = context.getBean(OpenTelemetryResourceBuilderCustomizer.class);
            ResourceBuilder builder = Resource.getDefault().toBuilder();
            builder.put("host.name", "test-host");
            builder.put("host.id", "test-id");
            builder.put("process.pid", "123");
            builder.put("service.name", "test-service");

            customizer.customize(builder);
            Resource resource = builder.build();

            // Attributes starting with disabled prefixes should be removed
            assertThat(resource.getAttribute(AttributeKey.stringKey("host.name"))).isNull();
            assertThat(resource.getAttribute(AttributeKey.stringKey("host.id"))).isNull();
            assertThat(resource.getAttribute(AttributeKey.stringKey("process.pid"))).isNull();
            // Other attributes should remain
            assertThat(resource.getAttribute(AttributeKey.stringKey("service.name"))).isEqualTo("test-service");
        });
    }

    @Test
    void attributeFilteringNotAppliedWhenAllEnabled() {
        contextRunner.withPropertyValues(
                "arconia.otel.resource.enable.all=true"
        ).run(context -> {
            OpenTelemetryResourceBuilderCustomizer customizer = context.getBean(OpenTelemetryResourceBuilderCustomizer.class);
            ResourceBuilder builder = Resource.getDefault().toBuilder();
            builder.put("host.name", "test-host");
            builder.put("process.pid", "123");
            builder.put("service.name", "test-service");

            customizer.customize(builder);
            Resource resource = builder.build();

            // All attributes should be preserved when all=true
            assertThat(resource.getAttribute(AttributeKey.stringKey("host.name"))).isEqualTo("test-host");
            assertThat(resource.getAttribute(AttributeKey.stringKey("process.pid"))).isEqualTo("123");
            assertThat(resource.getAttribute(AttributeKey.stringKey("service.name"))).isEqualTo("test-service");
        });
    }

    @Test
    void allAttributesRemovedWhenAllDisabled() {
        contextRunner.withPropertyValues(
                "arconia.otel.resource.enable.all=false"
        ).run(context -> {
            OpenTelemetryResourceBuilderCustomizer customizer = context.getBean(OpenTelemetryResourceBuilderCustomizer.class);
            ResourceBuilder builder = Resource.getDefault().toBuilder();
            builder.put("host.name", "test-host");
            builder.put("process.pid", "123");
            builder.put("service.name", "test-service");

            customizer.customize(builder);
            Resource resource = builder.build();

            // All attributes should be removed when all=false
            assertThat(resource.getAttribute(AttributeKey.stringKey("host.name"))).isNull();
            assertThat(resource.getAttribute(AttributeKey.stringKey("process.pid"))).isNull();
            assertThat(resource.getAttribute(AttributeKey.stringKey("service.name"))).isNull();
        });
    }

    @Test
    void resourceContributorsAndCustomizersAppliedInOrder() {
        contextRunner
                .withPropertyValues("arconia.otel.resource.attributes.custom.attribute=test-value")
                .withUserConfiguration(OrderedCustomizerConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    Resource resource = context.getBean(Resource.class);

                    // Verify the final resource has attributes from both contributors and customizers
                    assertThat(resource.getAttribute(AttributeKey.stringKey("custom.attribute"))).isEqualTo("another-value");
                    assertThat(context.getBean(EnvironmentResourceContributor.class)).isNotNull();
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedCustomizerConfiguration {

        @Bean
        OpenTelemetryResourceBuilderCustomizer orderedCustomizer() {
            return builder -> builder.put("custom.attribute", "another-value");
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class CustomResourceConfiguration {

        @Bean
        Resource customResource() {
            return Resource.empty();
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class BuildPropertiesConfiguration {

        @Bean
        BuildProperties buildProperties() {
            Properties properties = new Properties();
            properties.setProperty("version", "1.0.0");
            return new BuildProperties(properties);
        }

    }

}
