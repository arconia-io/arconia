package io.arconia.dev.services.core.registration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.env.DefaultPropertiesPropertySource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.AnnotationMetadata;

import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServicesRegistry.BeanName;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestLinkContainer;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DevServicesRegistrar}.
 */
class DevServicesRegistrarTests {

    private final DefaultListableBeanFactory beanDefinitionRegistry = new DefaultListableBeanFactory();

    private final StandardEnvironment environment = new StandardEnvironment();

    @Test
    void whenRegistrarRunsThenDevServicesAreRegistered() {
        doRegister(DevServicesTestSupport::registerPostgresDevService);

        assertThat(beanDefinitionRegistry.getBeanDefinitionNames())
                .containsExactlyInAnyOrder(BeanName.CONTAINER.of("postgres"), BeanName.REGISTRATION.of("postgres"));
    }

    @Test
    void whenMultipleRegistrarsRunThenTheyRegisterInTheSameBeanFactory() {
        doRegister(
                DevServicesTestSupport::registerPostgresDevService,
                registry -> registry.registerDevService(service -> service
                        .name("linky")
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestLinkContainer.class, TestLinkContainer::new)));

        assertThat(beanDefinitionRegistry.getBeanDefinitionNames()).containsExactlyInAnyOrder(
                BeanName.CONTAINER.of("postgres"), BeanName.REGISTRATION.of("postgres"),
                BeanName.CONTAINER.of("linky"), BeanName.REGISTRATION.of("linky"));
    }

    @Test
    void whenDuplicateRegistrationFromAnotherRegistrarThenThrow() {
        assertThatIllegalStateException()
                .isThrownBy(() -> doRegister(
                        DevServicesTestSupport::registerPostgresDevService,
                        DevServicesTestSupport::registerPostgresDevService))
                .withMessageContaining("A dev service named 'postgres' is already registered");
    }

    @Test
    void whenRegistrarsRegisterConflictingDevServicesThenStartupFails() {
        new ApplicationContextRunner()
                .withUserConfiguration(ConflictingConfiguration.class)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(MultipleDevServicesException.class)
                            .hasMessageContaining("jdbc")
                            .hasMessageContaining("first")
                            .hasMessageContaining("second");
                });
    }

    // --- setDefaultProperty ---

    @Test
    void setDefaultPropertyAddsToDefaultPropertiesSource() {
        TestRegistrar registrar = new TestRegistrar(registry -> {}, environment, beanDefinitionRegistry);
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(this.getClass()), beanDefinitionRegistry);

        registrar.setDefaultProperty("spring.datasource.url", "jdbc:h2:mem:test");

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:test");
    }

    @Test
    void setDefaultPropertyKeepsAnExistingValue() {
        DefaultPropertiesPropertySource.addOrMerge(Map.of("spring.datasource.url", "jdbc:h2:mem:app"), environment.getPropertySources());
        TestRegistrar registrar = new TestRegistrar(registry -> {}, environment, beanDefinitionRegistry);

        registrar.setDefaultProperty("spring.datasource.url", "jdbc:h2:mem:test");

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:app");
    }

    // --- addDynamicProperty ---

    @Test
    void addDynamicPropertyResolvesTheValueLazilyWithTheHighestPrecedence() {
        environment.getPropertySources().addFirst(new MapPropertySource("user", Map.of("acme.fancydb.url", "configured")));
        TestRegistrar registrar = new TestRegistrar(registry -> {}, environment, beanDefinitionRegistry);
        AtomicReference<String> url = new AtomicReference<>("http://localhost:1234");

        registrar.addDynamicProperty("acme.fancydb.url", url::get);

        // The value is read from the supplier on each access, and wins over the configured one.
        assertThat(environment.getProperty("acme.fancydb.url")).isEqualTo("http://localhost:1234");
        url.set("http://localhost:5678");
        assertThat(environment.getProperty("acme.fancydb.url")).isEqualTo("http://localhost:5678");
        assertThat(environment.getPropertySources().iterator().next().getName()).isEqualTo(DevServicesRegistrar.DYNAMIC_PROPERTY_SOURCE_NAME);
    }

    @SafeVarargs
    private void doRegister(Consumer<DevServicesRegistry>... registrars) {
        for (Consumer<DevServicesRegistry> consumer : registrars) {
            TestRegistrar registrar = new TestRegistrar(consumer, environment, beanDefinitionRegistry);
            registrar.registerBeanDefinitions(AnnotationMetadata.introspect(this.getClass()), beanDefinitionRegistry);
        }
    }

    private static class TestRegistrar extends DevServicesRegistrar {

        private final Consumer<DevServicesRegistry> registrar;

        TestRegistrar(Consumer<DevServicesRegistry> registrar, Environment environment, DefaultListableBeanFactory beanDefinitionRegistry) {
            this.registrar = registrar;
            setEnvironment(environment);
            setBeanFactory(beanDefinitionRegistry);
            setResourceLoader(new DefaultResourceLoader());
        }

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registrar.accept(registry);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({FirstJdbcDevServiceRegistrar.class, SecondJdbcDevServiceRegistrar.class})
    static class ConflictingConfiguration {}

    static class FirstJdbcDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("first")
                    .category("jdbc")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new));
        }

    }

    static class SecondJdbcDevServiceRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registry.registerDevService(service -> service
                    .name("second")
                    .category("jdbc")
                    .description("Test Dev Service")
                    .properties(TestDevServicesProperties.DEFAULT)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new));
        }

    }

}
