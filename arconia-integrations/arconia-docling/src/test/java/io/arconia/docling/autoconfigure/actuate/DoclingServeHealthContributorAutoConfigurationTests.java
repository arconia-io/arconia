package io.arconia.docling.autoconfigure.actuate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.docling.actuate.DoclingServeHealthIndicator;
import io.arconia.docling.autoconfigure.DoclingAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DoclingServeHealthContributorAutoConfiguration}.
 */
class DoclingServeHealthContributorAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class, DoclingAutoConfiguration.class,
                    DoclingServeHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

    @Test
    void runShouldCreateIndicator() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DoclingServeHealthIndicator.class).hasBean("doclingHealthContributor");
        });
    }

    @Test
    void runWhenDisabledShouldNotCreateIndicator() {
        contextRunner.withPropertyValues("management.health.docling.enabled:false").run(context -> {
            assertThat(context).doesNotHaveBean(DoclingServeHealthIndicator.class).doesNotHaveBean("doclingHealthContributor");
        });
    }

    @Test
    void runWithoutActuatorShouldNotCreateIndicator() {
        contextRunner.withClassLoader(new FilteredClassLoader(HealthContributor.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DoclingServeHealthIndicator.class)
                            .doesNotHaveBean("doclingHealthContributor");
                });
        contextRunner.withClassLoader(new FilteredClassLoader(CompositeHealthContributorConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DoclingServeHealthIndicator.class)
                            .doesNotHaveBean("doclingHealthContributor");
                });
        contextRunner.withClassLoader(new FilteredClassLoader(ConditionalOnEnabledHealthIndicator.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DoclingServeHealthIndicator.class)
                            .doesNotHaveBean("doclingHealthContributor");
                });
    }

    @Test
    void runWithoutDoclingClientBeanShouldNotCreateIndicator() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DoclingServeHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DoclingServeHealthIndicator.class).doesNotHaveBean("doclingHealthContributor");
                });
    }

}
