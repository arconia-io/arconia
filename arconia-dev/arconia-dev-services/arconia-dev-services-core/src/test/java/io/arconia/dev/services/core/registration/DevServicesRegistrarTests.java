package io.arconia.dev.services.core.registration;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.AnnotationMetadata;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.TestDevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DevServicesRegistrar}.
 */
class DevServicesRegistrarTests {

    private final DefaultListableBeanFactory beanDefinitionRegistry = new DefaultListableBeanFactory();

    private final StandardEnvironment environment = new StandardEnvironment();

    @Test
    void basicRegistration() {
        doRegister(registry -> registry.registerDevService(service ->
                service.name("docling")
                        .description("Docling")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestDoclingContainer.class, TestDoclingContainer::new)
                        .serviceConnectionName("docling")));
        assertContainerBeanDefinition("docling", TestDoclingContainer.class, "docling");
        assertDescriptionBeanDefinition("docling");
        assertBeanDefinitionCount(2);
    }

    @Test
    void multipleRegistrationsFromMultipleRegistryInvocations() {
        doRegister(
                registry -> registry.registerDevService(service ->
                        service.name("docling")
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)
                                .container(TestDoclingContainer.class, TestDoclingContainer::new)),
                registry -> registry.registerDevService(service ->
                        service.name("postgres")
                        .description("Test Dev Service")
                        .properties(TestDevServicesProperties.DEFAULT)
                                .container(TestPostgresContainer.class, TestPostgresContainer::new)));
        assertContainerBeanDefinition("docling", TestDoclingContainer.class, null);
        assertDescriptionBeanDefinition("docling");
        assertContainerBeanDefinition("postgres", TestPostgresContainer.class, null);
        assertDescriptionBeanDefinition("postgres");
        assertBeanDefinitionCount(4);
    }

    @Test
    void whenDuplicateRegistrationFromAnotherRegistrarThenThrow() {
        assertThatIllegalStateException().isThrownBy(() -> doRegister(
                registry -> registry.registerDevService(service ->
                        service.name("docling")
                                .description("Test Dev Service")
                                .properties(TestDevServicesProperties.DEFAULT)
                                .container(TestDoclingContainer.class, TestDoclingContainer::new)),
                registry -> registry.registerDevService(service ->
                        service.name("docling")
                                .description("Test Dev Service")
                                .properties(TestDevServicesProperties.DEFAULT)
                                .container(TestDoclingContainer.class, TestDoclingContainer::new))))
                .withMessageContaining("A dev service named 'docling' is already registered");
    }

    // --- setDefaultProperty ---

    @Test
    void setDefaultPropertyAddsToDefaultPropertiesSource() {
        TestRegistrar registrar = new TestRegistrar(registry -> {}, environment, beanDefinitionRegistry);
        registrar.registerBeanDefinitions(AnnotationMetadata.introspect(this.getClass()), beanDefinitionRegistry);

        registrar.setDefaultProperty("spring.datasource.url", "jdbc:h2:mem:test");

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:h2:mem:test");
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

    private void assertContainerBeanDefinition(String serviceName, Class<?> containerType, String serviceConnectionName) {
        String beanName = "devService.container." + serviceName;
        assertThat(beanDefinitionRegistry.containsBeanDefinition(beanName)).isTrue();

        BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanName);
        assertThat(beanDefinition.getBeanClassName()).isEqualTo(containerType.getName());
        assertThat(beanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_INFRASTRUCTURE);

        if (beanDefinition instanceof DevServiceContainerBeanDefinition devServiceBeanDefinition) {
            MergedAnnotations mergedAnnotations = devServiceBeanDefinition.getAnnotations();
            assertThat(mergedAnnotations.isPresent(ServiceConnection.class)).isTrue();

            if (serviceConnectionName != null) {
                ServiceConnection annotation = mergedAnnotations.get(ServiceConnection.class).synthesize();
                assertThat(annotation.value()).isEqualTo(serviceConnectionName);
            }
        }
    }

    private void assertDescriptionBeanDefinition(String serviceName) {
        String beanName = "devService.registration." + serviceName;
        assertThat(beanDefinitionRegistry.containsBeanDefinition(beanName)).isTrue();

        BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(beanName);
        assertThat(beanDefinition.getBeanClassName()).isEqualTo(DevServiceRegistration.class.getName());
        assertThat(beanDefinition.getRole()).isEqualTo(BeanDefinition.ROLE_SUPPORT);

        String[] dependsOn = beanDefinition.getDependsOn();
        assertThat(dependsOn).isNotNull();
        assertThat(dependsOn).contains("devService.container." + serviceName);
    }

    private void assertBeanDefinitionCount(int count) {
        assertThat(beanDefinitionRegistry.getBeanDefinitionCount()).isEqualTo(count);
    }

    private static class TestRegistrar extends DevServicesRegistrar {

        private final Consumer<DevServicesRegistry> registrar;

        TestRegistrar(Consumer<DevServicesRegistry> registrar, Environment environment, DefaultListableBeanFactory beanDefinitionRegistry) {
            this.registrar = registrar;
            setEnvironment(environment);
            setBeanFactory(beanDefinitionRegistry);
        }

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            registrar.accept(registry);
        }
    }

    private static class TestDoclingContainer extends GenericContainer<TestDoclingContainer> {
        TestDoclingContainer() {
            super("docling");
        }
    }

    private static class TestPostgresContainer extends GenericContainer<TestPostgresContainer> {
        TestPostgresContainer() {
            super("postgres");
        }
    }

}
