package io.arconia.dev.services.core.registration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.dockerjava.api.DockerClient;

import com.github.dockerjava.api.model.ContainerPort;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;

import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.provider.DevServiceProvider;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.autoconfigure.DevServicesConflictValidator;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;

/**
 * Registry for managing the definition and lifecycle of dev services.
 */
@Incubating
public class DevServicesRegistry {

    /**
     * The bean name of the {@link DevServicesConflictValidator} that container beans depend on.
     */
    public static final String CONFLICT_VALIDATOR_BEAN_NAME = "devService.conflictValidator";

    private static final String DEVTOOLS_RESTART_ENABLED_PROPERTY = "spring.devtools.restart.enabled";

    private final BeanDefinitionRegistry beanDefinitionRegistry;

    private final Environment environment;

    public DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment) {
        Assert.notNull(beanDefinitionRegistry, "beanDefinitionRegistry cannot be null");
        Assert.notNull(environment, "environment cannot be null");
        this.beanDefinitionRegistry = beanDefinitionRegistry;
        this.environment = environment;
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

        // 1. Register the conflict validator bean that every container depends on,
        // so that mutually exclusive dev services fail fast before any container is created.
        registerConflictValidatorBeanDefinition();

        // 2. Register the container bean
        String containerBeanName = "devService.container." + service.getName();
        if (!beanDefinitionRegistry.containsBeanDefinition(containerBeanName)) {
            GenericBeanDefinition containerBeanDefinition = createContainerBeanDefinition(service);
            beanDefinitionRegistry.registerBeanDefinition(containerBeanName, containerBeanDefinition);
        }

        // 3. Register the description bean
        String descriptionBeanName = "devServiceRegistration." + service.getName();
        if (!this.beanDefinitionRegistry.containsBeanDefinition(descriptionBeanName)) {
            RootBeanDefinition descriptionBeanDefinition = createDescriptionBeanDefinition(service, containerBeanName);
            this.beanDefinitionRegistry.registerBeanDefinition(descriptionBeanName, descriptionBeanDefinition);
        }

    }

    /**
     * Register a bean validating that no two dev services in the same mutually exclusive
     * category are active. Container beans depend on this bean, so validation happens
     * before any dev service container is created.
     */
    private void registerConflictValidatorBeanDefinition() {
        if (beanDefinitionRegistry.containsBeanDefinition(CONFLICT_VALIDATOR_BEAN_NAME)) {
            return;
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServicesConflictValidator.class);
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        beanDefinition.setInstanceSupplier((InstanceSupplier<DevServicesConflictValidator>) registeredBean -> {
            DevServicesConflictValidator validator = new DevServicesConflictValidator();
            if (registeredBean.getBeanFactory() instanceof ConfigurableListableBeanFactory listableBeanFactory) {
                validator.validate(listableBeanFactory.getBeansOfType(DevServiceProvider.class).values());
            }
            return validator;
        });

        beanDefinitionRegistry.registerBeanDefinition(CONFLICT_VALIDATOR_BEAN_NAME, beanDefinition);
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

        if (containerSpec.isServiceConnectionSupported()) {
            Map<String, Object> annotationAttributes = new HashMap<>();
            if (StringUtils.hasText(containerSpec.getServiceConnectionName())) {
                // Sets the "value" attribute for the @ServiceConnection annotation
                annotationAttributes.put("value", containerSpec.getServiceConnectionName());
            }
            beanDefinition.setAnnotations(MergedAnnotations.from(
                    AnnotationUtils.synthesizeAnnotation(annotationAttributes, ServiceConnection.class, null)));
        }

        // Provide a supplier for creating a Container instance.
        if (containerSpec.getSupplier() != null) {
            beanDefinition.setInstanceSupplier((InstanceSupplier<Container<?>>) registeredBean -> {
                Container<?> container = containerSpec.getSupplier().get();
                applyCustomizers(container, registeredBean.getBeanFactory());
                return container;
            });
        }

        // Handle restart scope if Spring Boot DevTools is present and restart support is not disabled.
        if (isRestartScopeAvailable()) {
            beanDefinition.setScope("restart");
        } else {
            beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
        }

        // Hint that this bean has an infrastructure role, meaning it has no relevance to the end-user.
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        // Ensure mutually exclusive dev services are validated before the container is created.
        beanDefinition.setDependsOn(CONFLICT_VALIDATOR_BEAN_NAME);

        return beanDefinition;
    }

    /**
     * Whether dev service containers should live in the {@code restart} scope, so that they
     * survive Spring Boot DevTools restarts. That's the case when DevTools is on the classpath
     * and restart support is not disabled via the {@code spring.devtools.restart.enabled} property.
     */
    private boolean isRestartScopeAvailable() {
        ClassLoader classLoader = (beanDefinitionRegistry instanceof ConfigurableBeanFactory beanFactory)
                ? beanFactory.getBeanClassLoader() : ClassUtils.getDefaultClassLoader();
        return ClassUtils.isPresent("org.springframework.boot.devtools.restart.RestartScope", classLoader)
                && environment.getProperty(DEVTOOLS_RESTART_ENABLED_PROPERTY, Boolean.class, true);
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
     * Apply all matching {@link DevServiceContainerCustomizer} beans to the given container,
     * in {@code @Order} semantics, before the container is started.
     */
    @SuppressWarnings("unchecked")
    private static void applyCustomizers(Container<?> container, ConfigurableBeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory listableBeanFactory)) {
            return;
        }

        // Map customizer instances to their bean names so their generic type can be resolved
        // from the bean definition, which works even for lambdas returned from @Bean methods.
        Map<DevServiceContainerCustomizer<?>, String> beanNamesByCustomizer = new IdentityHashMap<>();
        for (String beanName : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(listableBeanFactory, DevServiceContainerCustomizer.class)) {
            beanNamesByCustomizer.put(listableBeanFactory.getBean(beanName, DevServiceContainerCustomizer.class), beanName);
        }

        listableBeanFactory.getBeanProvider(DevServiceContainerCustomizer.class).orderedStream()
                .filter(customizer -> supportsContainer(listableBeanFactory, beanNamesByCustomizer.get(customizer), customizer, container))
                .forEach(customizer -> ((DevServiceContainerCustomizer<Container<?>>) customizer).customize(container));
    }

    /**
     * Whether the given customizer applies to the given container, based on the customizer's
     * generic type. The type is resolved from the bean definition (so that lambdas returned
     * from {@code @Bean} methods work) with a fallback on the customizer's class. If the type
     * cannot be resolved, the customizer applies to all containers.
     */
    private static boolean supportsContainer(ConfigurableListableBeanFactory beanFactory, @Nullable String beanName,
            DevServiceContainerCustomizer<?> customizer, Container<?> container) {
        ResolvableType customizerType = ResolvableType.NONE;
        if (beanName != null && beanFactory.containsBeanDefinition(beanName)) {
            customizerType = beanFactory.getMergedBeanDefinition(beanName).getResolvableType();
        }

        Class<?> targetType = customizerType.as(DevServiceContainerCustomizer.class).getGeneric(0).resolve();
        if (targetType == null) {
            targetType = GenericTypeResolver.resolveTypeArgument(customizer.getClass(), DevServiceContainerCustomizer.class);
        }

        return targetType == null || targetType.isInstance(container);
    }

    /**
     * Extract container information by querying the OCI runtime using the container ID.
     */
    private static ContainerInfo extractContainerInfoById(String containerId) {
        try {
            // Get Docker client from Testcontainers. We don't close the connection as it's handled
            // globally by the DockerClientFactory.
            DockerClient dockerClient = DockerClientFactory.lazyClient();
            // Query Docker for the container using its ID
            com.github.dockerjava.api.model.Container dockerContainer = dockerClient.listContainersCmd()
                    .withIdFilter(Collections.singleton(containerId))
                    .withShowAll(true)
                    .exec()
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Container not found with ID: " + containerId));

            List<String> names = Arrays.stream(dockerContainer.getNames() != null ? dockerContainer.getNames() : new String[0])
                    .map(name -> name.charAt(0) == '/' ? name.substring(1) : name)
                    .toList();
            String imageName = dockerContainer.getImage();
            Map<String, String> labels = dockerContainer.getLabels() != null ? dockerContainer.getLabels() : Map.of();
            String status = dockerContainer.getStatus();

            List<ContainerInfo.ContainerPort> exposedPorts = Arrays.stream(
                            dockerContainer.getPorts() != null ? dockerContainer.getPorts() : new ContainerPort[0])
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

        private boolean serviceConnectionSupported = true;

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
         * <p>
         * By default, {@code @ServiceConnection} is added with no explicit name,
         * and Spring Boot auto-detects the connection details factory by container type.
         * <p>
         * Passing a non-null value sets the {@code @ServiceConnection} name explicitly.
         * Passing {@code null} disables {@code @ServiceConnection} entirely, for cases where
         * no {@code ContainerConnectionDetailsFactory} is available and property-based wiring
         * (via {@link DevServiceDynamicPropertySource}) is used instead.
         */
        public ContainerSpec serviceConnectionName(@Nullable String name) {
            this.serviceConnectionName = name;
            this.serviceConnectionSupported = (name != null);
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

        boolean isServiceConnectionSupported() {
            return serviceConnectionSupported;
        }

        @Nullable
        String getServiceConnectionName() {
            return serviceConnectionName;
        }

    }

}
