package io.arconia.dev.services.core.registration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.TestcontainersConfiguration;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.autoconfigure.DevServicesProperties;
import io.arconia.dev.services.core.container.ContainerConfigurer;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;

/**
 * Registry for managing the definition and lifecycle of dev services.
 */
@Incubating
public class DevServicesRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DevServicesRegistry.class);

    private static final String CONTAINER_BEAN_NAME_PREFIX = "devService.container.";

    private static final String CONNECTION_DETAILS_BEAN_NAME_PREFIX = "devService.connectionDetails.";

    private static final String REGISTRATION_BEAN_NAME_PREFIX = "devService.registration.";

    /**
     * The attribute of a registration bean definition holding the category of the dev service.
     */
    private static final String CATEGORY_ATTRIBUTE = "devService.category";

    /**
     * The attribute marking a dev service bean definition as excluded from AOT processing.
     */
    static final String AOT_EXCLUDED_ATTRIBUTE = "devService.aotExcluded";

    private final BeanDefinitionRegistry beanDefinitionRegistry;

    private final Environment environment;

    private final ContainerDiscovery containerDiscovery;

    private final DevServiceRestartSupport restartSupport;

    DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment) {
        this(beanDefinitionRegistry, environment, new DockerContainerDiscovery());
    }

    DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment, ContainerDiscovery containerDiscovery) {
        Assert.notNull(beanDefinitionRegistry, "beanDefinitionRegistry cannot be null");
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(containerDiscovery, "containerDiscovery cannot be null");
        this.beanDefinitionRegistry = beanDefinitionRegistry;
        this.environment = environment;
        this.containerDiscovery = containerDiscovery;
        this.restartSupport = new DevServiceRestartSupport(beanDefinitionRegistry, environment);
    }

    /**
     * Register a single dev service.
     *
     * @param service consumer to configure the service specification
     */
    public void registerDevService(Consumer<ServiceSpec> service) {
        ServiceSpec serviceSpec = new ServiceSpec();
        service.accept(serviceSpec);
        registerBeanDefinitions(serviceSpec);
    }

    /**
     * Register all the beans associated with a dev service.
     */
    private void registerBeanDefinitions(ServiceSpec service) {
        service.validate();
        ReuseDecision reuse = reuseDecision(service);

        String containerBeanName = CONTAINER_BEAN_NAME_PREFIX + service.getName();
        String registrationBeanName = REGISTRATION_BEAN_NAME_PREFIX + service.getName();

        // 1. Ensure a dev service registered more than once in the same context (e.g. by two
        // registrars) is registered once. Only registering it again with a different container
        // type is an error.
        if (isRegistrationBeanAlreadyDefined(registrationBeanName)) {
            validateContainerBeanDefinition(service, containerBeanName);
            return;
        }

        // 2. Ensure no other dev service of the same category is registered, since dev services
        // providing the same capability are mutually exclusive.
        ensureCategoryIsFree(service);

        // 3. Check whether the application has kept a running container for this dev service across a
        // DevTools restart. If it has, the application keeps using it.
        boolean containerKept = restartSupport.retainKeptContainer(service.getName(), containerBeanName);

        // 4. If no container was kept, look for a container started by another application, when
        // the framework reuse strategy applies. If one is discovered, the beans for it are
        // registered instead, and the application does not start a container of its own.
        if (!containerKept && registerDiscoveredBeanDefinitions(service, reuse, registrationBeanName)) {
            return;
        }

        // 5. Register the container bean definition. If a container was kept across a DevTools
        // restart, the bean resolves to it. Otherwise, a new container is created and started,
        // meaning that the application owns its lifecycle.
        beanDefinitionRegistry.registerBeanDefinition(containerBeanName, createContainerBeanDefinition(service, reuse));

        // 6. Register the registration bean definition describing the dev service. It depends on
        // the container bean, so that the container is started before the dev service is reported
        // as ready.
        beanDefinitionRegistry.registerBeanDefinition(registrationBeanName, createRegistrationBeanDefinition(service, reuse, containerBeanName, containerKept));
    }

    private boolean isRegistrationBeanAlreadyDefined(String registrationBeanName) {
        return beanDefinitionRegistry.containsBeanDefinition(registrationBeanName);
    }

    private void validateContainerBeanDefinition(ServiceSpec service, String containerBeanName) {
        if (beanDefinitionRegistry.containsBeanDefinition(containerBeanName)) {
            String registeredType = beanDefinitionRegistry.getBeanDefinition(containerBeanName).getBeanClassName();
            Assert.state(service.getContainerType().getName().equals(registeredType),
                    () -> "A dev service named '%s' is already registered with container type %s, cannot register another one with container type %s"
                            .formatted(service.getName(), registeredType, service.getContainerType().getName()));
        }
    }

    /**
     * Fail if a dev service of the same category as the given one is already registered.
     * The category of a registered dev service is recorded on its registration bean definition.
     */
    private void ensureCategoryIsFree(ServiceSpec service) {
        String category = service.getCategory();
        if (category == null) {
            return;
        }
        for (String beanName : beanDefinitionRegistry.getBeanDefinitionNames()) {
            if (beanName.startsWith(REGISTRATION_BEAN_NAME_PREFIX)
                    && category.equals(beanDefinitionRegistry.getBeanDefinition(beanName).getAttribute(CATEGORY_ATTRIBUTE))) {
                String otherName = beanName.substring(REGISTRATION_BEAN_NAME_PREFIX.length());
                throw new MultipleDevServicesException(category, Stream.of(otherName, service.getName()).sorted().toList());
            }
        }
    }

    // REUSE

    record ReuseDecision(ReuseStrategy effective, @Nullable String reason) {}

    ReuseDecision reuseDecision(ServiceSpec service) {
        ReuseStrategy configured = service.getProperties().getReuseStrategy();
        if (configured == ReuseStrategy.NONE || !BootstrapMode.isDev()) {
            return new ReuseDecision(ReuseStrategy.NONE, null);
        }
        if (configured == ReuseStrategy.FRAMEWORK && !service.supportsDiscovery()) {
            return new ReuseDecision(ReuseStrategy.NONE, "'framework' is not supported by this dev service");
        }
        if (configured == ReuseStrategy.TESTCONTAINERS && !environmentSupportsTestcontainersReuse()) {
            return new ReuseDecision(ReuseStrategy.NONE, "'testcontainers' requires 'testcontainers.reuse.enable=true' in ~/.testcontainers.properties");
        }
        return new ReuseDecision(configured, null);
    }

    boolean environmentSupportsTestcontainersReuse() {
        return TestcontainersConfiguration.getInstance().environmentSupportsReuse();
    }

    // DISCOVERED

    /**
     * Register the beans associated with a dev service running in a container started by another
     * application, if one is discovered, and returns whether that happened.
     */
    private boolean registerDiscoveredBeanDefinitions(ServiceSpec service, ReuseDecision reuse, String registrationBeanName) {
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

        // 3. A ConnectionDetails bean of the declared type defined by the application takes
        // precedence over the one provided by the dev service, mirroring Spring Boot's service
        // connection behavior. The oldest candidate is reported as is: how to connect to it is
        // decided by the application's bean.
        String[] existingBeanNames = getExistingConnectionDetailsBeanNames(connectionDetailsType);
        if (existingBeanNames.length > 0) {
            logger.debug("The '{}' dev service provides no connection details for the discovered container: the application defines its own in {}",
                    service.getName(), Arrays.asList(existingBeanNames));
            beanDefinitionRegistry.registerBeanDefinition(registrationBeanName,
                    createDiscoveredRegistrationBeanDefinition(service, candidates.getFirst()));
            return true;
        }

        // 4. Register a ConnectionDetails bean for each discovered container and a bean describing the dev service.
        // Candidates are processed from oldest to newest. If a container cannot be connected to, the candidate is skipped.
        for (DiscoveredContainer candidate : candidates) {
            try {
                ConnectionDetails connectionDetails = connectionDetailsFactory.apply(candidate);
                Assert.state(connectionDetailsType.isInstance(connectionDetails), "connectionDetails must be an instance of " + connectionDetailsType.getName());
                beanDefinitionRegistry.registerBeanDefinition(CONNECTION_DETAILS_BEAN_NAME_PREFIX + service.getName(),
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

    private String[] getExistingConnectionDetailsBeanNames(Class<?> connectionDetailsType) {
        if (beanDefinitionRegistry instanceof ListableBeanFactory listableBeanFactory) {
            return listableBeanFactory.getBeanNamesForType(connectionDetailsType, true, false);
        }
        return new String[0];
    }

    /**
     * Create the bean definition for the {@link ConnectionDetails} of a dev service running in a
     * discovered container. The bean takes the place of the container-based connection details
     * that Spring Boot would produce for an owned container via the {@code @ServiceConnection} mechanism.
     */
    private RootBeanDefinition createDiscoveredConnectionDetailsBeanDefinition(ConnectionDetails connectionDetails, ContainerInfo containerInfo) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        excludeFromAot(beanDefinition);
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
     * Unlike the owned variant, the container ID comes straight from the discovery,
     * so the bean doesn't depend on any container bean.
     */
    private RootBeanDefinition createDiscoveredRegistrationBeanDefinition(ServiceSpec service, DiscoveredContainer discoveredContainer) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServiceRegistration.class);
        excludeFromAot(beanDefinition);
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        recordCategory(beanDefinition, service);

        ContainerInfo containerInfo = discoveredContainer.containerInfo();
        beanDefinition.setInstanceSupplier(() -> {
            // The links are resolved against the ports the discovered container publishes.
            List<DevServiceLink> links = DevServiceLinks.resolve(service, discoveredContainer.host(), discoveredContainer::mappedPort, false);
            return ready(service, DevServiceRegistration.Origin.DISCOVERED, containerInfo.id(), containerInfo.imageName(), links, false,
                    new ReuseDecision(ReuseStrategy.FRAMEWORK, null));
        });

        return beanDefinition;
    }

    /**
     * Log that the dev service is ready and describe it for the application. Called once the
     * container is known to be running, so the startup message and the description agree.
     */
    private static DevServiceRegistration ready(ServiceSpec service, DevServiceRegistration.Origin origin, @Nullable String containerId,
            @Nullable String imageName, List<DevServiceLink> links, boolean kept, @Nullable ReuseDecision reuse) {
        DevServiceStartupLogger.ready(service.getName(), containerId, imageName, origin, kept, reuse, links);
        return DevServiceRegistration.builder()
                .name(service.getName())
                .description(service.getDescription())
                .origin(origin)
                .containerInfo(() -> ContainerRuntimeInfo.extractContainerInfoById(Objects.requireNonNull(containerId, "containerId")))
                .links(links)
                .build();
    }

    // OWNED

    private GenericBeanDefinition createContainerBeanDefinition(ServiceSpec service, ReuseDecision reuse) {
        Supplier<? extends GenericContainer<?>> containerSupplier = service.getContainerSupplier();

        // Create container bean definition.
        DevServiceContainerBeanDefinition beanDefinition = new DevServiceContainerBeanDefinition();
        excludeFromAot(beanDefinition);
        beanDefinition.setBeanClass(service.getContainerType());

        // Set description if provided.
        if (service.getDescription() != null) {
            beanDefinition.setDescription(service.getDescription());
        }

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
            ContainerConfigurer.apply(container, service.getProperties(), reuse.effective() == ReuseStrategy.TESTCONTAINERS);
            applyCustomizers(container, registeredBean.getBeanFactory());
            applyLabels(container, service, reuse);
            applyNetwork(container, service, registeredBean.getBeanFactory());
            return container;
        });

        // With Spring Boot DevTools, the container is kept across application restarts.
        beanDefinition.setScope(restartSupport.isRestartScopeActive() ? DevServiceRestartSupport.SCOPE_NAME : BeanDefinition.SCOPE_SINGLETON);

        // Hint that this bean has an infrastructure role, meaning it has no relevance to the end-user.
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

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
     * Mark the given dev service bean definition as excluded from AOT processing: it is created
     * by an instance supplier and decided at startup from the state of the container runtime,
     * neither of which can be generated ahead of time.
     *
     * @see DevServicesBeanRegistrationExcludeFilter
     */
    private static void excludeFromAot(BeanDefinition beanDefinition) {
        beanDefinition.setAttribute(AOT_EXCLUDED_ATTRIBUTE, true);
    }

    /**
     * The image of a started container as the container runtime reports it, which Testcontainers
     * caches; the image reference is not used, since resolving it may pull the image.
     */
    @Nullable
    private static String imageNameOf(GenericContainer<?> container) {
        InspectContainerResponse containerInfo = container.getContainerInfo();
        return (containerInfo != null && containerInfo.getConfig() != null) ? containerInfo.getConfig().getImage() : null;
    }

    private static void recordCategory(BeanDefinition beanDefinition, ServiceSpec service) {
        if (service.getCategory() != null) {
            beanDefinition.setAttribute(CATEGORY_ATTRIBUTE, service.getCategory());
        }
    }

    private static void applyLabels(GenericContainer<?> container, ServiceSpec service, ReuseDecision reuse) {
        container.withLabel(DevServiceLabels.NAME, service.getName());
        if (reuse.effective() == ReuseStrategy.FRAMEWORK) {
            container.withLabel(DevServiceLabels.DISCOVERABLE, "true");
            container.withLabel(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
        }
    }

    /**
     * Attach the given container to the {@link Network} shared by the dev service containers of
     * the application, when enabled.
     * <p>
     * The service name is added as a network alias, so every container is reachable by a stable,
     * predictable name next to any {@code network-aliases} configured for it. A network already
     * set on the container (e.g. by a customizer) is honored.
     * <p>
     * Port mapping is left untouched: the application keeps reaching the container
     * over the host and mapped ports.
     */
    private void applyNetwork(GenericContainer<?> container, ServiceSpec service, ConfigurableBeanFactory beanFactory) {
        if (!isNetworkEnabled()) {
            return;
        }

        Network network = beanFactory.getBean(Network.class);
        if (container.getNetwork() == null) {
            container.withNetwork(network);
        }
        container.withNetworkAliases(service.getName());

        if (container.isShouldBeReused() && network == Network.SHARED) {
            logger.warn("The 'testcontainers' reuse strategy is ineffective for the '{}' dev service on the per-application network. Define a Network bean with a stable id to combine them", service.getName());
        }
    }

    /**
     * Whether dev service containers should join the shared network (global
     * {@code arconia.dev.services.network.enabled}, default {@code false}).
     */
    private boolean isNetworkEnabled() {
        return devServicesProperties().getNetwork().isEnabled();
    }

    /**
     * The global dev services configuration, bound from the environment.
     */
    private DevServicesProperties devServicesProperties() {
        return Binder.get(environment).bindOrCreate(DevServicesProperties.CONFIG_PREFIX, DevServicesProperties.class);
    }

    private RootBeanDefinition createRegistrationBeanDefinition(ServiceSpec service, ReuseDecision reuse, String containerBeanName, boolean containerKept) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServiceRegistration.class);
        excludeFromAot(beanDefinition);
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        beanDefinition.setDependsOn(containerBeanName);
        recordCategory(beanDefinition, service);

        beanDefinition.setInstanceSupplier((InstanceSupplier<DevServiceRegistration>) registeredBean -> {
            // The container is started at this point, so the links can be resolved against its
            // mapped ports. The reuse strategy is only reported in dev mode, the only one where it applies.
            GenericContainer<?> container = registeredBean.getBeanFactory().getBean(containerBeanName, GenericContainer.class);
            List<DevServiceLink> links = DevServiceLinks.resolve(service, container.getHost(), container::getMappedPort, true);
            return ready(service, DevServiceRegistration.Origin.OWNED, container.getContainerId(), imageNameOf(container), links,
                    containerKept, BootstrapMode.isDev() ? reuse : null);
        });

        return beanDefinition;
    }

}
