package io.arconia.dev.services.core.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import io.arconia.dev.services.api.provider.DevServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesAutoConfiguration}.
 */
class DevServicesAutoConfigurationTests {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DevServicesAutoConfiguration.class));

    @Test
    void propertiesBeanIsAvailable() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(DevServicesProperties.class));
    }

    @Test
    void noConflictWithNoProviders() {
        contextRunner.run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void noConflictWithSingleProvider() {
        contextRunner
                .withBean("lgtm", DevServiceProvider.class, () -> DevServiceProvider.of("lgtm", "opentelemetry"))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void noConflictWithProvidersInDifferentCategories() {
        contextRunner
                .withBean("lgtm", DevServiceProvider.class, () -> DevServiceProvider.of("lgtm", "opentelemetry"))
                .withBean("postgresql", DevServiceProvider.class, () -> DevServiceProvider.of("postgresql", "jdbc"))
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void conflictDetectedWithMultipleProvidersInSameCategory() {
        contextRunner
                .withBean("lgtm", DevServiceProvider.class, () -> DevServiceProvider.of("lgtm", "opentelemetry"))
                .withBean("openlit", DevServiceProvider.class, () -> DevServiceProvider.of("openlit", "opentelemetry"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(MultipleDevServicesException.class);
                });
    }

}

