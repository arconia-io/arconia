package io.arconia.dev.services.core.registration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.dockerjava.api.command.InspectContainerResponse;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;

/**
 * Registry for managing the definition and lifecycle of dev services.
 */
@Incubating
public final class DevServicesRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DevServicesRegistry.class);

    private final BeanDefinitionRegistry beanDefinitionRegistry;

    private final ContainerDiscovery containerDiscovery;

    private final BooleanSupplier testcontainersReuseSupport;

    private final DevServiceRestartSupport restartSupport;

    private final DevServiceNetworkSupport networkSupport;

    DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment) {
        this(beanDefinitionRegistry, environment, new DockerContainerDiscovery());
    }

    DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment, ContainerDiscovery containerDiscovery) {
        this(beanDefinitionRegistry, environment, containerDiscovery, () -> TestcontainersConfiguration.getInstance().environmentSupportsReuse());
    }

    /**
     * Create a registry whose Testcontainers reuse support check is the given one instead of reading the local Testcontainers configuration.
     */
    DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment, ContainerDiscovery containerDiscovery, BooleanSupplier testcontainersReuseSupport) {
        Assert.notNull(beanDefinitionRegistry, "beanDefinitionRegistry cannot be null");
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(containerDiscovery, "containerDiscovery cannot be null");
        Assert.notNull(testcontainersReuseSupport, "testcontainersReuseSupport cannot be null");
        this.beanDefinitionRegistry = beanDefinitionRegistry;
        this.containerDiscovery = containerDiscovery;
        this.testcontainersReuseSupport = testcontainersReuseSupport;
        this.restartSupport = new DevServiceRestartSupport(beanDefinitionRegistry, environment);
        this.networkSupport = new DevServiceNetworkSupport(environment);
    }

    /**
     * Register a single dev service.
     *
     * @param service consumer to configure the service specification
     */
    public void registerDevService(Consumer<DevServiceSpec> service) {
        DevServiceSpec serviceSpec = new DevServiceSpec();
        service.accept(serviceSpec);
        registerBeanDefinitions(serviceSpec);
    }

    /**
     * Register all the beans associated with a dev service.
     */
    private void registerBeanDefinitions(DevServiceSpec service) {
        service.validate();
        ReuseDecision reuse = ReuseDecision.decide(service, testcontainersReuseSupport);

        String containerBeanName = BeanName.CONTAINER.of(service.getName());
        String registrationBeanName = BeanName.REGISTRATION.of(service.getName());

        // 1. A dev service is registered once per application:
        // registering another one with the same name is an error.
        Assert.state(!beanDefinitionRegistry.containsBeanDefinition(registrationBeanName),
                () -> "A dev service named '%s' is already registered".formatted(service.getName()));

        // 2. Ensure no other dev service of the same category is registered, since dev services
        // providing the same capability are mutually exclusive.
        ensureCategoryIsFree(service);

        // 3. A ConnectionDetails bean of the declared type defined by the application takes
        // precedence over the dev service, mirroring Spring Boot's service connection behavior:
        // the application connects to the service of its choice, so the dev service is neither
        // started nor registered.
        if (isConnectionDetailsDefinedByApplication(service)) {
            return;
        }

        // 4. Check whether the application has kept a running container for this dev service across a
        // DevTools restart. If it has, the application keeps using it.
        boolean containerKept = restartSupport.retainKeptContainer(service.getName(), containerBeanName, service.getContainerType());

        // 5. If no container was kept, look for a container started by another application, when
        // the framework reuse strategy applies. If one is discovered, the beans for it are
        // registered instead, and the application does not start a container of its own.
        if (!containerKept && registerDiscoveredBeanDefinitions(service, reuse, registrationBeanName)) {
            return;
        }

        // 6. Register the container bean definition. If a container was kept across a DevTools
        // restart, the bean resolves to it. Otherwise, a new container is created and started,
        // meaning that the application manages its lifecycle.
        beanDefinitionRegistry.registerBeanDefinition(containerBeanName, createContainerBeanDefinition(service, reuse));

        // 7. Register the registration bean definition describing the dev service. It depends on
        // the container bean, so that the container is started before the dev service is reported
        // as ready.
        beanDefinitionRegistry.registerBeanDefinition(registrationBeanName, createRegistrationBeanDefinition(service, reuse, containerBeanName, containerKept));
    }

    /**
     * Fail if a dev service of the same category as the given one is already registered.
     * The category of a registered dev service is recorded on its registration bean definition.
     */
    private void ensureCategoryIsFree(DevServiceSpec service) {
        String category = service.getCategory();
        if (category == null) {
            return;
        }
        for (String beanName : beanDefinitionRegistry.getBeanDefinitionNames()) {
            if (BeanName.REGISTRATION.matches(beanName)
                    && category.equals(BeanAttribute.CATEGORY.get(beanDefinitionRegistry.getBeanDefinition(beanName)))) {
                String otherName = BeanName.REGISTRATION.serviceName(beanName);
                throw new MultipleDevServicesException(category, Stream.of(otherName, service.getName()).sorted().toList());
            }
        }
    }

    /**
     * Whether the application defines a {@link ConnectionDetails} bean of the type the dev
     * service declares. Only a dev service declaring its discovery states that type.
     */
    private boolean isConnectionDetailsDefinedByApplication(DevServiceSpec service) {
        Class<? extends ConnectionDetails> connectionDetailsType = service.getConnectionDetailsType();
        if (connectionDetailsType == null || !(beanDefinitionRegistry instanceof ListableBeanFactory listableBeanFactory)) {
            return false;
        }
        String[] existingBeanNames = listableBeanFactory.getBeanNamesForType(connectionDetailsType, true, false);
        if (existingBeanNames.length == 0) {
            return false;
        }
        logger.info("Dev Service '{}' is not started: the application defines its own {} in {}",
                service.getName(), connectionDetailsType.getSimpleName(), Arrays.asList(existingBeanNames));
        return true;
    }

    // DISCOVERED

    /**
     * Register the beans associated with a dev service running in a container started by another
     * application, if one is discovered, and returns whether that happened.
     */
    private boolean registerDiscoveredBeanDefinitions(DevServiceSpec service, ReuseDecision reuse, String registrationBeanName) {
        // 1. If not using the framework reuse strategy, no container is discovered.
        if (reuse.effective() != ReuseStrategy.FRAMEWORK) {
            return false;
        }
        // The framework strategy is only in effect when the dev service declares its discovery.
        Class<? extends ConnectionDetails> connectionDetailsType = service.getConnectionDetailsType();
        Function<DiscoveredContainer, ? extends ConnectionDetails> connectionDetailsFactory = service.getConnectionDetailsFactory();
        Assert.state(connectionDetailsType != null && connectionDetailsFactory != null, "discovery must be declared");

        // 2. If no container is discovered, the application starts its own container.
        List<DiscoveredContainer> candidates = containerDiscovery.discover(service.getName());
        if (candidates.isEmpty()) {
            return false;
        }

        // 3. Register a ConnectionDetails bean for each discovered container and a bean describing the dev service.
        // Candidates are processed from oldest to newest. If a container cannot be connected to, the candidate is skipped.
        for (DiscoveredContainer candidate : candidates) {
            try {
                ConnectionDetails connectionDetails = connectionDetailsFactory.apply(candidate);
                Assert.state(connectionDetailsType.isInstance(connectionDetails), "connectionDetails must be an instance of " + connectionDetailsType.getName());
                beanDefinitionRegistry.registerBeanDefinition(BeanName.CONNECTION_DETAILS.of(service.getName()),
                        createDiscoveredConnectionDetailsBeanDefinition(connectionDetails, candidate.containerInfo()));
                beanDefinitionRegistry.registerBeanDefinition(registrationBeanName,
                        createDiscoveredRegistrationBeanDefinition(service, candidate));
                return true;
            } catch (Exception ex) {
                logger.warn("Failed to build the connection details for the '{}' dev service in the discovered container {}. Skipping it.",
                        service.getName(), ContainerRuntimeInfo.shortId(candidate.containerInfo().id()), ex);
            }
        }

        logger.info("No discovered container can be used for the '{}' dev service. Starting its own container instead.", service.getName());
        return false;
    }

    /**
     * Create the bean definition for the {@link ConnectionDetails} of a dev service running in a
     * discovered container. The bean takes the place of the container-based connection details
     * that Spring Boot would produce for a managed container via the {@code @ServiceConnection} mechanism.
     */
    private static RootBeanDefinition createDiscoveredConnectionDetailsBeanDefinition(ConnectionDetails connectionDetails, ContainerInfo containerInfo) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        BeanAttribute.AOT_EXCLUDED.set(beanDefinition, true);
        beanDefinition.setBeanClass(connectionDetails.getClass());
        beanDefinition.setInstanceSupplier(() -> connectionDetails);
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        // Attach the container image metadata, letting downstream auto-configurations
        // introspect which image backs the connection details bean.
        new ContainerImageMetadata(containerInfo.imageName()).addTo(beanDefinition);

        return beanDefinition;
    }

    /**
     * Create the bean definition describing a dev service running in a discovered container.
     * Unlike the managed variant, the container ID comes straight from the discovery,
     * so the bean doesn't depend on any container bean.
     */
    private static RootBeanDefinition createDiscoveredRegistrationBeanDefinition(DevServiceSpec service, DiscoveredContainer discoveredContainer) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServiceRegistration.class);
        BeanAttribute.AOT_EXCLUDED.set(beanDefinition, true);
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        recordCategory(beanDefinition, service);

        ContainerInfo containerInfo = discoveredContainer.containerInfo();
        beanDefinition.setInstanceSupplier(() -> {
            // The links are resolved against the ports the discovered container publishes.
            List<DevServiceLink> links = DevServiceLinks.resolve(service, discoveredContainer.host(), discoveredContainer::mappedPort,
                    DevServiceRegistration.Origin.DISCOVERED);
            DevServiceStartupLogger.ready(service.getName(), containerInfo.id(), containerInfo.imageName(),
                    DevServiceRegistration.Origin.DISCOVERED, false, new ReuseDecision(ReuseStrategy.FRAMEWORK, null), links);
            return createRegistration(service, DevServiceRegistration.Origin.DISCOVERED, containerInfo.id(), links);
        });

        return beanDefinition;
    }

    // MANAGED

    private GenericBeanDefinition createContainerBeanDefinition(DevServiceSpec service, ReuseDecision reuse) {
        Supplier<? extends GenericContainer<?>> containerSupplier = service.getContainerSupplier();

        // Create container bean definition.
        DevServiceContainerBeanDefinition beanDefinition = new DevServiceContainerBeanDefinition();
        BeanAttribute.AOT_EXCLUDED.set(beanDefinition, true);
        beanDefinition.setBeanClass(service.getContainerType());
        beanDefinition.setDescription(service.getDescription());

        if (service.isServiceConnection()) {
            Map<String, Object> annotationAttributes = new HashMap<>();
            if (StringUtils.hasText(service.getServiceConnectionName())) {
                // Sets the "value" attribute for the @ServiceConnection annotation
                annotationAttributes.put("value", service.getServiceConnectionName());
            }
            beanDefinition.setAnnotations(MergedAnnotations.from(
                    AnnotationUtils.synthesizeAnnotation(annotationAttributes, ServiceConnection.class, null)));
        }

        // Provide a supplier for creating a Container instance.
        beanDefinition.setInstanceSupplier((InstanceSupplier<GenericContainer<?>>) registeredBean -> {
            GenericContainer<?> container = containerSupplier.get();
            ContainerConfigurer.apply(container, service.getProperties());
            container.withReuse(reuse.effective() == ReuseStrategy.TESTCONTAINERS);
            applyCustomizers(container, registeredBean.getBeanFactory());
            applyLabels(container, service, reuse);
            networkSupport.join(container, service.getName(), registeredBean.getBeanFactory());
            return container;
        });

        // With Spring Boot DevTools, the container is kept across application restarts.
        beanDefinition.setScope(restartSupport.isRestartScopeActive() ? DevServiceRestartSupport.SCOPE_NAME : BeanDefinition.SCOPE_SINGLETON);

        // Hint that this bean has an infrastructure role, meaning it has no relevance to the end-user.
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        return beanDefinition;
    }

    private RootBeanDefinition createRegistrationBeanDefinition(DevServiceSpec service, ReuseDecision reuse, String containerBeanName, boolean containerKept) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServiceRegistration.class);
        BeanAttribute.AOT_EXCLUDED.set(beanDefinition, true);
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        beanDefinition.setDependsOn(containerBeanName);
        recordCategory(beanDefinition, service);

        beanDefinition.setInstanceSupplier((InstanceSupplier<DevServiceRegistration>) registeredBean -> {
            // The container is started at this point, so the links can be resolved against its mapped ports.
            GenericContainer<?> container = registeredBean.getBeanFactory().getBean(containerBeanName, GenericContainer.class);
            List<DevServiceLink> links = DevServiceLinks.resolve(service, container.getHost(), container::getMappedPort,
                    DevServiceRegistration.Origin.MANAGED);
            // The reuse strategy is only reported in dev mode, the only one where it applies.
            DevServiceStartupLogger.ready(service.getName(), container.getContainerId(), imageNameOf(container),
                    DevServiceRegistration.Origin.MANAGED, containerKept, BootstrapMode.isDev() ? reuse : null, links);
            return createRegistration(service, DevServiceRegistration.Origin.MANAGED, container.getContainerId(), links);
        });

        return beanDefinition;
    }

    /**
     * Apply all matching {@link DevServiceContainerCustomizer} beans to the given container,
     * in {@code @Order} semantics, before the container is started. A customizer matches when
     * the container is an instance of its generic type. When that type cannot be resolved, as
     * for a lambda, the customizer is invoked and skipped if the container doesn't match, the
     * same way Spring Boot applies its own customizers.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void applyCustomizers(Container<?> container, ConfigurableBeanFactory beanFactory) {
        List<DevServiceContainerCustomizer> customizers = beanFactory.getBeanProvider(DevServiceContainerCustomizer.class)
                .orderedStream()
                .toList();
        LambdaSafe.callbacks(DevServiceContainerCustomizer.class, customizers, container)
                .withLogger(DevServicesRegistry.class)
                .invoke(customizer -> customizer.customize(container));
    }

    /**
     * Label the container with the name of its dev service and, when it is reusable via the
     * framework, mark it as discoverable by other applications on behalf of this one.
     */
    private static void applyLabels(GenericContainer<?> container, DevServiceSpec service, ReuseDecision reuse) {
        container.withLabel(DevServiceLabels.NAME, service.getName());
        if (reuse.effective() == ReuseStrategy.FRAMEWORK) {
            container.withLabel(DevServiceLabels.DISCOVERABLE, "true");
            container.withLabel(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
        }
    }

    /**
     * Record the category of the dev service on its registration bean definition, so that
     * another dev service of the same category is detected at registration time.
     */
    private static void recordCategory(BeanDefinition beanDefinition, DevServiceSpec service) {
        if (service.getCategory() != null) {
            BeanAttribute.CATEGORY.set(beanDefinition, service.getCategory());
        }
    }

    /**
     * The image of a started container as the container runtime reports it, which Testcontainers
     * caches. The image reference is not used, since resolving it may pull the image.
     */
    @Nullable
    private static String imageNameOf(GenericContainer<?> container) {
        InspectContainerResponse containerInfo = container.getContainerInfo();
        return (containerInfo != null && containerInfo.getConfig() != null) ? containerInfo.getConfig().getImage() : null;
    }

    /**
     * Create the registration describing the dev service for the application, running in the
     * container with the given ID. The container information is resolved from the container
     * runtime on demand, so that the current state of the container is reported.
     */
    private static DevServiceRegistration createRegistration(DevServiceSpec service, DevServiceRegistration.Origin origin,
            @Nullable String containerId, List<DevServiceLink> links) {
        return DevServiceRegistration.builder()
                .name(service.getName())
                .description(service.getDescription())
                .origin(origin)
                .containerInfo(() -> {
                    Assert.notNull(containerId, "containerId cannot be null");
                    return ContainerRuntimeInfo.extractContainerInfoById(containerId);
                })
                .links(links)
                .build();
    }

    /**
     * The beans registered for a dev service, named after it.
     */
    enum BeanName {

        CONTAINER("container"),

        CONNECTION_DETAILS("connectionDetails"),

        REGISTRATION("registration");

        /**
         * The prefix shared by the names of all dev service beans.
         */
        static final String PREFIX = "devService.";

        private final String prefix;

        BeanName(String kind) {
            this.prefix = PREFIX + kind + ".";
        }

        /**
         * The name of this bean for the dev service with the given name.
         */
        String of(String serviceName) {
            return prefix + serviceName;
        }

        /**
         * Whether the given bean name is the name of this bean for some dev service.
         */
        boolean matches(String beanName) {
            return beanName.startsWith(prefix);
        }

        /**
         * The name of the dev service this bean name belongs to.
         */
        String serviceName(String beanName) {
            Assert.isTrue(matches(beanName), () -> "'%s' is not a %s bean name".formatted(beanName, name()));
            return beanName.substring(prefix.length());
        }

    }

    /**
     * The attributes recorded on dev service bean definitions.
     */
    enum BeanAttribute {

        /**
         * The category of the dev service, recorded on its registration bean definition.
         */
        CATEGORY("devService.category"),

        /**
         * Marks a dev service bean definition as excluded from AOT processing: the bean is
         * created by an instance supplier and decided at startup from the state of the
         * container runtime, neither of which can be generated ahead of time.
         *
         * @see DevServicesBeanRegistrationExcludeFilter
         */
        AOT_EXCLUDED("devService.aotExcluded");

        private final String key;

        BeanAttribute(String key) {
            this.key = key;
        }

        void set(BeanDefinition beanDefinition, Object value) {
            beanDefinition.setAttribute(key, value);
        }

        @Nullable
        Object get(BeanDefinition beanDefinition) {
            return beanDefinition.getAttribute(key);
        }

        boolean isPresent(BeanDefinition beanDefinition) {
            return beanDefinition.hasAttribute(key);
        }

    }

}
