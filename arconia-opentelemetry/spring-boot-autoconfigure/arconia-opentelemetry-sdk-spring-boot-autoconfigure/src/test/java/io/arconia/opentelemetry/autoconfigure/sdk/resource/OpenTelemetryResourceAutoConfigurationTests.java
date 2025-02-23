package io.arconia.opentelemetry.autoconfigure.sdk.resource;

import java.util.Properties;

import io.opentelemetry.sdk.resources.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.BuildResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.EnvironmentResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.FilterResourceContributor;
import io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor.MapResourceContributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.InstanceOfAssertFactories.MAP;

/**
 * Unit tests for {@link OpenTelemetryResourceAutoConfiguration}.
 */
class OpenTelemetryResourceAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenTelemetryResourceAutoConfiguration.class))
            .withPropertyValues("arconia.opentelemetry.enabled=true");

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        contextRunner
            .withPropertyValues("arconia.opentelemetry.enabled=false")
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
            assertThat(context).hasSingleBean(MapResourceContributor.class);
            assertThat(context).hasSingleBean(EnvironmentResourceContributor.class);
            assertThat(context).doesNotHaveBean(BuildResourceContributor.class);
            assertThat(context).hasSingleBean(FilterResourceContributor.class);
        });
    }

    @Test
    void environmentResourceContributorCreatedWhenEnabled() {
        contextRunner.withPropertyValues("arconia.opentelemetry.resource.environment.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).hasSingleBean(EnvironmentResourceContributor.class);
                });
    }

    @Test
    void environmentResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.opentelemetry.resource.environment.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(EnvironmentResourceContributor.class);
                });
    }

    @Test
    void buildResourceContributorCreatedWhenEnabledAndBuildPropertiesAvailable() {
        contextRunner.withPropertyValues("arconia.opentelemetry.resource.build.enabled=true")
                .withUserConfiguration(BuildPropertiesConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).hasSingleBean(BuildResourceContributor.class);
                });
    }

    @Test
    void buildResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.opentelemetry.resource.build.enabled=false")
                .withUserConfiguration(BuildPropertiesConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(BuildResourceContributor.class);
                });
    }

    @Test
    void buildResourceContributorNotCreatedWhenBuildPropertiesNotAvailable() {
        contextRunner.withPropertyValues("arconia.opentelemetry.resource.build.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(BuildResourceContributor.class);
                });
    }

    @Test
    void filterResourceContributorCreatedWhenEnabled() {
        contextRunner.withPropertyValues("arconia.opentelemetry.resource.filter.enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).hasSingleBean(FilterResourceContributor.class);
                });
    }

    @Test
    void filterResourceContributorNotCreatedWhenDisabled() {
        contextRunner.withPropertyValues("arconia.opentelemetry.resource.filter.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(Resource.class);
                    assertThat(context).doesNotHaveBean(FilterResourceContributor.class);
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
    void attributesAreConfiguredOnMapResourceContributor() {
        contextRunner.withPropertyValues(
                "arconia.opentelemetry.resource.attributes.key1=value1",
                "arconia.opentelemetry.resource.attributes.key2=value2"
        ).run(context -> {
            assertThat(context).hasSingleBean(Resource.class);
            MapResourceContributor contributor = context.getBean(MapResourceContributor.class);
            assertThat(contributor).extracting("attributes", MAP)
                    .containsEntry("key1", "value1")
                    .containsEntry("key2", "value2");
        });
    }

    @Test
    void disabledKeysAreConfiguredOnFilterResourceContributor() {
        contextRunner.withPropertyValues(
                "arconia.opentelemetry.resource.filter.enabled=true",
                "arconia.opentelemetry.resource.filter.disabled-keys[0]=key1",
                "arconia.opentelemetry.resource.filter.disabled-keys[1]=key2"
        ).run(context -> {
            assertThat(context).hasSingleBean(Resource.class);
            FilterResourceContributor contributor = context.getBean(FilterResourceContributor.class);
            assertThat(contributor).extracting("disabledKeys", LIST)
                    .containsExactly("key1", "key2");
        });
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
