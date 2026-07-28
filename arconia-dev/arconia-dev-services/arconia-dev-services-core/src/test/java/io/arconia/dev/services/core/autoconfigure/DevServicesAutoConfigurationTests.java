package io.arconia.dev.services.core.autoconfigure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Network;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.provider.DevServiceProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesAutoConfiguration}.
 */
class DevServicesAutoConfigurationTests {

    private static final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DevServicesAutoConfiguration.class));

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
        BootstrapMode.clear();
    }

    @Test
    void propertiesBeanIsAvailable() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(DevServicesProperties.class));
    }

    @Test
    void networkBeanDefaultsToSharedNetwork() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "test");
        BootstrapMode.clear();
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Network.class);
            assertThat(context.getBean(Network.class)).isSameAs(Network.SHARED);
        });
    }

    @Test
    void userNetworkBeanTakesPrecedence() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "test");
        BootstrapMode.clear();
        Network userNetwork = new TestNetwork();
        contextRunner
                .withBean("userNetwork", Network.class, () -> userNetwork)
                .run(context -> {
                    assertThat(context).hasSingleBean(Network.class);
                    assertThat(context.getBean(Network.class)).isSameAs(userNetwork);
                });
    }

    @Test
    void networkBeanIsNotCreatedInProdMode() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "prod");
        BootstrapMode.clear();
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(Network.class));
    }

    @Test
    void conflictValidatorBeanIsNotCreatedInProdMode() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "prod");
        BootstrapMode.clear();
        contextRunner.run(context -> assertThat(context).doesNotHaveBean("devServicesConflictValidator"));
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

    private record TestNetwork() implements Network {
        @Override
        public String getId() {
            return "test-network";
        }

        @Override
        public void close() {
        }
    }

}

