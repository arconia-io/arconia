package io.arconia.docling.autoconfigure.actuate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.docling.actuate.DoclingHealthIndicator;
import io.arconia.docling.autoconfigure.client.DoclingClientAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DoclingHealthContributorAutoConfiguration}.
 */
class DoclingHealthContributorAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class, DoclingClientAutoConfiguration.class,
                    DoclingHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class));

    @Test
    void runShouldCreateIndicator() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(DoclingHealthIndicator.class).hasBean("doclingHealthContributor");
        });
    }

    @Test
    void runWhenDisabledShouldNotCreateIndicator() {
        contextRunner.withPropertyValues("management.health.docling.enabled:false").run(context -> {
            assertThat(context).doesNotHaveBean(DoclingHealthIndicator.class).doesNotHaveBean("doclingHealthContributor");
        });
    }

    @Test
    void runWithoutDoclingClientBeanShouldNotCreateIndicator() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DoclingHealthContributorAutoConfiguration.class, HealthContributorAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DoclingHealthIndicator.class).doesNotHaveBean("doclingHealthContributor");
                });
    }

}
