package io.arconia.dev.services.core.registration;

import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.AnnotationMetadata;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.registration.DevServiceRegistration;

import static org.assertj.core.api.Assertions.assertThat;

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
                        .container(container -> container
                                .type(TestDoclingContainer.class)
                                .supplier(TestDoclingContainer::new)
                                .serviceConnectionName("docling"))));

        assertRegistryExists();
        assertContainerBeanDefinition("docling", TestDoclingContainer.class, "docling");
        assertDescriptionBeanDefinition("docling");
        assertBeanDefinitionCount(2);
    }

    @Test
    void multipleRegistrationsFromSingleRegistryInvocation() {
        doRegister(registry -> {
            registry.registerDevService(service ->
                    service.name("docling")
                            .description("Docling")
                            .container(container -> container
                                    .type(TestDoclingContainer.class)
                                    .supplier(TestDoclingContainer::new)));

            registry.registerDevService(service ->
                    service.name("postgres")
                            .description("PostgreSQL database")
                            .container(container -> container
                                    .type(TestPostgresContainer.class)
                                    .supplier(TestPostgresContainer::new)));
        });

        assertRegistryExists();
        assertContainerBeanDefinition("docling", TestDoclingContainer.class, null);
        assertDescriptionBeanDefinition("docling");
        assertContainerBeanDefinition("postgres", TestPostgresContainer.class, null);
        assertDescriptionBeanDefinition("postgres");
        assertBeanDefinitionCount(4);
    }

    @Test
    void multipleRegistrationsFromMultipleRegistryInvocations() {
        doRegister(
                registry -> registry.registerDevService(service ->
                        service.name("docling")
                                .container(container -> container
                                        .type(TestDoclingContainer.class)
                                        .supplier(TestDoclingContainer::new))),
                registry -> registry.registerDevService(service ->
                        service.name("postgres")
                                .container(container -> container
                                        .type(TestPostgresContainer.class)
                                        .supplier(TestPostgresContainer::new))));

        assertRegistryExists();
        assertContainerBeanDefinition("docling", TestDoclingContainer.class, null);
        assertDescriptionBeanDefinition("docling");
        assertContainerBeanDefinition("postgres", TestPostgresContainer.class, null);
        assertDescriptionBeanDefinition("postgres");
        assertBeanDefinitionCount(4);
    }

    @Test
    void whenDuplicateRegistrationThenSkipSecond() {
        doRegister(
                registry -> registry.registerDevService(service ->
                        service.name("docling")
                                .container(container -> container
                                        .type(TestDoclingContainer.class)
                                        .serviceConnectionName("firstdocling")
                                        .supplier(TestDoclingContainer::new))),
                registry -> registry.registerDevService(service ->
                        service.name("docling")
                                .container(container -> container
                                        .type(TestDoclingContainer.class)
                                        .serviceConnectionName("stilldocling")
                                        .supplier(TestDoclingContainer::new))));

        assertRegistryExists();
        assertContainerBeanDefinition("docling", TestDoclingContainer.class, "firstdocling");
        assertDescriptionBeanDefinition("docling");
        assertBeanDefinitionCount(2);
    }

    @Test
    void noRegistrations() {
        doRegister(registry -> {});

        assertRegistryExists();
        assertBeanDefinitionCount(0);
    }

    @Test
    void registryBeanIsReusedAcrossDevServices() {
        doRegister(registry -> registry.registerDevService(service ->
                service.name("docling")
                        .container(container -> container
                                .type(TestDoclingContainer.class)
                                .supplier(TestDoclingContainer::new))));

        Map<String, DevServicesRegistry> registryBeansBefore = beanDefinitionRegistry.getBeansOfType(DevServicesRegistry.class);
        assertThat(registryBeansBefore).hasSize(1);

        doRegister(registry -> registry.registerDevService(service ->
                service.name("postgres")
                        .container(container -> container
                                .type(TestPostgresContainer.class)
                                .supplier(TestPostgresContainer::new))));

        Map<String, DevServicesRegistry> registryBeansAfter = beanDefinitionRegistry.getBeansOfType(DevServicesRegistry.class);
        assertThat(registryBeansAfter).hasSize(1);

        assertThat(registryBeansAfter.values().iterator().next()).isSameAs(registryBeansBefore.values().iterator().next());
    }

    @SafeVarargs
    private void doRegister(Consumer<DevServicesRegistry>... registrars) {
        for (Consumer<DevServicesRegistry> consumer : registrars) {
            TestRegistrar registrar = new TestRegistrar(consumer);
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
        String beanName = "devServiceRegistration." + serviceName;
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

    private void assertRegistryExists() {
        assertThat(beanDefinitionRegistry.containsSingleton(DevServicesRegistrar.DEV_SERVICES_REGISTRY_BEAN_NAME)).isTrue();
    }

    private class TestRegistrar extends DevServicesRegistrar {

        private final Consumer<DevServicesRegistry> registrar;

        TestRegistrar(Consumer<DevServicesRegistry> registrar) {
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
