package io.arconia.dev.services.core.registration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.dockerjava.api.DockerClient;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

/**
 * Registry for managing the definition and lifecycle of dev services.
 */
@Incubating
public class DevServicesRegistry {

    private final BeanDefinitionRegistry beanDefinitionRegistry;

    public DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry) {
        Assert.notNull(beanDefinitionRegistry, "beanDefinitionRegistry cannot be null");
        this.beanDefinitionRegistry = beanDefinitionRegistry;
    }

    /**
     * Register a single dev service.
     *
     * @param service consumer to configure the service specification
     */
    public void registerDevService(Consumer<ServiceSpec> service) {
        ServiceSpec serviceSpec = new ServiceSpec();
        service.accept(serviceSpec);
        registerBeanDefinition(serviceSpec);
    }

    /**
     * Register container and description beans if not present.
     */
    private void registerBeanDefinition(ServiceSpec service) {
        Assert.hasText(service.getName(), "service name cannot be null or empty");
        Assert.notNull(service.getContainerSpec(), "service container cannot be null");
        Assert.notNull(service.getContainerSpec().getType(), "service container type cannot be null");
        Assert.notNull(service.getContainerSpec().getSupplier(), "service container supplier cannot be null");

        // 1. Register the container bean
        String containerBeanName = "devService.container." + service.getName();
        if (!beanDefinitionRegistry.containsBeanDefinition(containerBeanName)) {
            GenericBeanDefinition containerBeanDefinition = createContainerBeanDefinition(service);
            beanDefinitionRegistry.registerBeanDefinition(containerBeanName, containerBeanDefinition);
        }

        // 2. Register the description bean
        String descriptionBeanName = "devServiceRegistration." + service.getName();
        if (!this.beanDefinitionRegistry.containsBeanDefinition(descriptionBeanName)) {
            RootBeanDefinition descriptionBeanDefinition = createDescriptionBeanDefinition(service, containerBeanName);
            this.beanDefinitionRegistry.registerBeanDefinition(descriptionBeanName, descriptionBeanDefinition);
        }

    }

    private GenericBeanDefinition createContainerBeanDefinition(ServiceSpec service) {
        ContainerSpec containerSpec = service.getContainerSpec();

        // Create container bean definition.
        DevServiceContainerBeanDefinition beanDefinition = new DevServiceContainerBeanDefinition();
        beanDefinition.setBeanClass(containerSpec.getType());

        // Set description if provided.
        if (service.getDescription() != null) {
            beanDefinition.setDescription(service.getDescription());
        }

        // Add @ServiceConnection annotation.
        Map<String, Object> annotationAttributes = new HashMap<>();
        if (StringUtils.hasText(containerSpec.getServiceConnectionName())) {
            // Sets the "value" attribute for the @ServiceConnection annotation
            annotationAttributes.put("value", containerSpec.getServiceConnectionName());
        }
        beanDefinition.setAnnotations(MergedAnnotations.from(
                AnnotationUtils.synthesizeAnnotation(annotationAttributes, ServiceConnection.class, null)));

        // Provide a supplier for creating a Container instance.
        if (containerSpec.getSupplier() != null) {
            beanDefinition.setInstanceSupplier((InstanceSupplier<Container<?>>) registeredBean ->
                    containerSpec.getSupplier().get());
        }

        // Handle restart scope if Spring Boot DevTools is present.
        if (ClassUtils.isPresent("org.springframework.boot.devtools.restart.RestartScope", null)) {
            beanDefinition.setScope("restart");
        } else {
            beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        }

        // Hint that this bean has an infrastructure role, meaning it has no relevance to the end-user.
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        return beanDefinition;
    }

    private RootBeanDefinition createDescriptionBeanDefinition(ServiceSpec service, String containerBeanName) {
        // Create description bean definition.
        RootBeanDefinition descriptionBeanDefinition = new RootBeanDefinition();
        descriptionBeanDefinition.setBeanClass(DevServiceRegistration.class);

        // Hint that this bean has a support role, meaning it is a supporting part of some larger configuration.
        descriptionBeanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);

        // Add dependency on the container bean so it's available when this bean is created.
        descriptionBeanDefinition.setDependsOn(containerBeanName);

        // Provide a supplier for creating a DevServiceRegistration instance.
        descriptionBeanDefinition.setInstanceSupplier((InstanceSupplier<DevServiceRegistration>) registeredBean -> {
            // Get the container bean from the bean factory to extract the container ID.
            Container<?> container = registeredBean.getBeanFactory().getBean(containerBeanName, Container.class);

            // Capture the container ID.
            String containerId = container.getContainerId();

            // Create a supplier that fetches container info from the OCI runtime API.
            Supplier<ContainerInfo> containerInfoSupplier = () -> extractContainerInfoById(containerId);

            return new DevServiceRegistration(
                    service.getName(),
                    service.getDescription(),
                    containerInfoSupplier
            );

        });

        return descriptionBeanDefinition;
    }

    /**
     * Extract container information by querying the OCI runtime using the container ID.
     */
    private static ContainerInfo extractContainerInfoById(String containerId) {
        try {
            // Get Docker client from Testcontainers
            com.github.dockerjava.api.model.Container dockerContainer;
            try (DockerClient dockerClient = DockerClientFactory.lazyClient()) {
                // Query Docker for the container using its ID
                dockerContainer = dockerClient.listContainersCmd()
                        .withIdFilter(Collections.singleton(containerId))
                        .withShowAll(true)
                        .exec()
                        .stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Container not found with ID: " + containerId));
            }

            List<String> names = List.of(dockerContainer.getNames());
            String imageName = dockerContainer.getImage();
            Map<String, String> labels = dockerContainer.getLabels();
            String status = dockerContainer.getStatus();

            List<ContainerInfo.ContainerPort> exposedPorts = Arrays.stream(dockerContainer.getPorts())
                    .map(port -> new ContainerInfo.ContainerPort(
                            port.getIp(),
                            port.getPrivatePort(),
                            port.getPublicPort(),
                            port.getType()
                    ))
                    .toList();

            return new ContainerInfo(containerId, imageName, names, exposedPorts, labels, status);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to extract container information for ID: " + containerId, ex);
        }
    }

    /**
     * Specification for a single dev service.
     */
    public static final class ServiceSpec {

        @Nullable
        private String name;

        @Nullable
        private String description;

        @Nullable
        private ContainerSpec containerSpec;

        private ServiceSpec() {}

        /**
         * The logical name of the dev service.
         */
        public ServiceSpec name(String name) {
            this.name = name;
            return this;
        }

        /**
         * The description of the dev service.
         */
        public ServiceSpec description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Specification for the container to register.
         */
        public ServiceSpec container(Consumer<ContainerSpec> containerSpecConsumer) {
            var containerSpec = new ContainerSpec();
            containerSpecConsumer.accept(containerSpec);
            this.containerSpec = containerSpec;
            return this;
        }

        @Nullable
        String getName() {
            return name;
        }

        @Nullable
        String getDescription() {
            return description;
        }

        @Nullable
        ContainerSpec getContainerSpec() {
            return containerSpec;
        }

    }

    /**
     * Specification for a container to register.
     */
    public static final class ContainerSpec {

        @Nullable
        private Class<? extends Container<?>> type;

        @Nullable
        private Supplier<? extends Container<?>> supplier;

        @Nullable
        private String serviceConnectionName;

        private ContainerSpec() {}

        /**
         * The container type to register.
         */
        public ContainerSpec type(Class<? extends Container<?>> type) {
            this.type = type;
            return this;
        }

        /**
         * A supplier function providing the container instance.
         */
        public ContainerSpec supplier(Supplier<? extends Container<?>> supplier) {
            this.supplier = supplier;
            return this;
        }

        /**
         * The name of the {@link ServiceConnection} annotation to add to the registered container bean.
         */
        public ContainerSpec serviceConnectionName(@Nullable String name) {
            this.serviceConnectionName = name;
            return this;
        }

        @Nullable
        Class<? extends Container<?>> getType() {
            return type;
        }

        @Nullable
        Supplier<? extends Container<?>> getSupplier() {
            return supplier;
        }

        @Nullable
        String getServiceConnectionName() {
            return serviceConnectionName;
        }

    }

}
