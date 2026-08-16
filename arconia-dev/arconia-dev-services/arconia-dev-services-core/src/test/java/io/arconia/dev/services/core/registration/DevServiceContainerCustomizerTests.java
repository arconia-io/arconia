package io.arconia.dev.services.core.registration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServiceContainerCustomizer}.
 */
class DevServiceContainerCustomizerTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context ->
                    context.getBeanFactory().registerScope("restart", new SimpleThreadScope()))
            .withUserConfiguration(DevServiceConfiguration.class);

    @Test
    void customizerAppliedToMatchingContainerType() {
        contextRunner.withUserConfiguration(MatchingCustomizerConfiguration.class)
                .run(context -> {
                    var container = context.getBean(TestPostgresContainer.class);
                    assertThat(container.getLabels()).containsEntry("customized", "true");
                });
    }

    @Test
    void customizerNotAppliedToNonMatchingContainerType() {
        contextRunner.withUserConfiguration(NonMatchingCustomizerConfiguration.class)
                .run(context -> {
                    var container = context.getBean(TestPostgresContainer.class);
                    assertThat(container.getLabels()).doesNotContainKey("customized");
                });
    }

    @Test
    void customizerForSupertypeAppliedToAllContainers() {
        contextRunner.withUserConfiguration(SupertypeCustomizerConfiguration.class)
                .run(context -> {
                    var container = context.getBean(TestPostgresContainer.class);
                    assertThat(container.getLabels()).containsEntry("generic", "true");
                });
    }

    @Test
    void customizerForIntermediateSupertypeAppliedToSubclassContainer() {
        new ApplicationContextRunner()
                .withInitializer(context ->
                        context.getBeanFactory().registerScope("restart", new SimpleThreadScope()))
                .withUserConfiguration(SubclassDevServiceConfiguration.class, MatchingCustomizerConfiguration.class)
                .run(context -> {
                    var container = context.getBean(TestPostgresSubclassContainer.class);
                    assertThat(container.getLabels()).containsEntry("customized", "true");
                });
    }

    @Test
    void customizersAppliedInOrder() {
        contextRunner.withUserConfiguration(OrderedCustomizersConfiguration.class)
                .run(context -> {
                    var container = context.getBean(TestPostgresContainer.class);
                    assertThat(container.getLabels()).containsEntry("order", "second");
                });
    }

    @Test
    void customizerImplementedAsClassApplied() {
        contextRunner.withUserConfiguration(ClassCustomizerConfiguration.class)
                .run(context -> {
                    var container = context.getBean(TestPostgresContainer.class);
                    assertThat(container.getLabels()).containsEntry("from-class", "true");
                });
    }

    @Test
    void noCustomizersRegistered() {
        contextRunner.run(context -> {
            var container = context.getBean(TestPostgresContainer.class);
            assertThat(container.getLabels()).doesNotContainKey("customized");
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import(TestDevServiceRegistrar.class)
    static class DevServiceConfiguration {}

    static class TestDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(container -> container
                            .type(TestPostgresContainer.class)
                            .supplier(TestPostgresContainer::new)));
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class MatchingCustomizerConfiguration {

        @Bean
        DevServiceContainerCustomizer<TestPostgresContainer> postgresCustomizer() {
            return container -> container.withLabel("customized", "true");
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class NonMatchingCustomizerConfiguration {

        @Bean
        DevServiceContainerCustomizer<TestRedisContainer> redisCustomizer() {
            return container -> container.withLabel("customized", "true");
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class SupertypeCustomizerConfiguration {

        @Bean
        DevServiceContainerCustomizer<GenericContainer<?>> genericCustomizer() {
            return container -> container.withLabel("generic", "true");
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class OrderedCustomizersConfiguration {

        @Bean
        @Order(2)
        DevServiceContainerCustomizer<TestPostgresContainer> secondCustomizer() {
            return container -> container.withLabel("order", "second");
        }

        @Bean
        @Order(1)
        DevServiceContainerCustomizer<TestPostgresContainer> firstCustomizer() {
            return container -> container.withLabel("order", "first");
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class ClassCustomizerConfiguration {

        @Bean
        PostgresLabelCustomizer postgresLabelCustomizer() {
            return new PostgresLabelCustomizer();
        }

    }

    static class PostgresLabelCustomizer implements DevServiceContainerCustomizer<TestPostgresContainer> {

        @Override
        public void customize(TestPostgresContainer container) {
            container.withLabel("from-class", "true");
        }

    }

    @Configuration(proxyBeanMethods = false)
    @Import(SubclassDevServiceRegistrar.class)
    static class SubclassDevServiceConfiguration {}

    static class SubclassDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("postgres")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(container -> container
                            .type(TestPostgresSubclassContainer.class)
                            .supplier(TestPostgresSubclassContainer::new)));
        }

    }

    static class TestPostgresContainer extends GenericContainer<TestPostgresContainer> {
        TestPostgresContainer() {
            super("postgres");
        }
    }

    static class TestPostgresSubclassContainer extends TestPostgresContainer {}

    static class TestRedisContainer extends GenericContainer<TestRedisContainer> {
        TestRedisContainer() {
            super("redis");
        }
    }

}
