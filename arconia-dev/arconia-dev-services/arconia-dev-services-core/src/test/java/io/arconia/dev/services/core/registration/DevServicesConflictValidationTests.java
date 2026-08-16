package io.arconia.dev.services.core.registration;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.provider.DevServiceProvider;
import io.arconia.dev.services.core.autoconfigure.MultipleDevServicesException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests verifying that mutually exclusive dev services are detected before any container is created.
 */
class DevServicesConflictValidationTests {

    private static final AtomicBoolean containerCreated = new AtomicBoolean(false);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context ->
                    context.getBeanFactory().registerScope("restart", new SimpleThreadScope()));

    @BeforeEach
    void setUp() {
        containerCreated.set(false);
    }

    @Test
    void conflictDetectedBeforeContainersAreCreated() {
        contextRunner.withUserConfiguration(ConflictingConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause()
                            .isInstanceOf(MultipleDevServicesException.class)
                            .hasMessageContaining("jdbc")
                            .hasMessageContaining("first")
                            .hasMessageContaining("second");
                    assertThat(containerCreated).isFalse();
                });
    }

    @Test
    void noConflictWithSingleProvider() {
        contextRunner.withUserConfiguration(SingleServiceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(containerCreated).isTrue();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({FirstDevServiceRegistrar.class, SecondDevServiceRegistrar.class})
    static class ConflictingConfiguration {

        @Bean
        DevServiceProvider firstDevServiceProvider() {
            return DevServiceProvider.of("first", "jdbc");
        }

        @Bean
        DevServiceProvider secondDevServiceProvider() {
            return DevServiceProvider.of("second", "jdbc");
        }

    }

    @Configuration(proxyBeanMethods = false)
    @Import(FirstDevServiceRegistrar.class)
    static class SingleServiceConfiguration {

        @Bean
        DevServiceProvider firstDevServiceProvider() {
            return DevServiceProvider.of("first", "jdbc");
        }

    }

    static class FirstDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("first")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(container -> container
                            .type(TestContainer.class)
                            .supplier(TestContainer::new)));
        }

    }

    static class SecondDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("second")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(container -> container
                            .type(TestContainer.class)
                            .supplier(TestContainer::new)));
        }

    }

    static class TestContainer extends GenericContainer<TestContainer> {
        TestContainer() {
            super("test");
            containerCreated.set(true);
        }
    }

}
