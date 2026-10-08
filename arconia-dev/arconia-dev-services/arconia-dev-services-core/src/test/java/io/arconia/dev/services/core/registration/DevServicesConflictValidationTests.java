package io.arconia.dev.services.core.registration;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.GenericContainer;


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
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(MultipleDevServicesException.class)
                            .hasMessageContaining("jdbc")
                            .hasMessageContaining("first")
                            .hasMessageContaining("second");
                    assertThat(containerCreated).isFalse();
                });
    }

    @Test
    void noConflictWithSingleService() {
        contextRunner.withUserConfiguration(SingleServiceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(containerCreated).isTrue();
                });
    }

    @Test
    void noConflictAcrossCategoriesOrWithoutCategory() {
        contextRunner.withUserConfiguration(CompatibleConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBeanNamesForType(GenericContainer.class)).hasSize(3);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({FirstDevServiceRegistrar.class, SecondDevServiceRegistrar.class})
    static class ConflictingConfiguration {}

    @Configuration(proxyBeanMethods = false)
    @Import(FirstDevServiceRegistrar.class)
    static class SingleServiceConfiguration {}

    @Configuration(proxyBeanMethods = false)
    @Import({FirstDevServiceRegistrar.class, OtherCategoryDevServiceRegistrar.class, UncategorizedDevServiceRegistrar.class})
    static class CompatibleConfiguration {}

    static class FirstDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("first")
                    .category("jdbc")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestContainer.class, TestContainer::new));
        }

    }

    static class SecondDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("second")
                    .category("jdbc")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestContainer.class, TestContainer::new));
        }

    }

    static class OtherCategoryDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("other")
                    .category("opentelemetry")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestContainer.class, TestContainer::new));
        }

    }

    static class UncategorizedDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("uncategorized")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestContainer.class, TestContainer::new));
        }

    }

    static class TestContainer extends GenericContainer<TestContainer> {
        TestContainer() {
            super("test");
            containerCreated.set(true);
        }
    }

}
