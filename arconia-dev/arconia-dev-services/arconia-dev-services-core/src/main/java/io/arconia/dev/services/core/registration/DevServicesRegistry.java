package io.arconia.dev.services.core.registration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
import io.arconia.dev.services.core.autoconfigure.MultipleDevServicesException;
import io.arconia.dev.services.core.autoconfigure.DevServicesProperties;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;

/**
 * Registry for managing the definition and lifecycle of dev services.
 */
@Incubating
public class DevServicesRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DevServicesRegistry.class);

    /**
     * The names a dev service can be given. The name becomes part of bean names, container
     * labels, and the default network alias of the container, so it is kept to the character
     * set valid in all of them.
     */
    private static final Pattern SERVICE_NAME_PATTERN = Pattern.compile("[a-z0-9]([a-z0-9-]*[a-z0-9])?");

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

    public DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment) {
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
        Assert.hasText(service.getName(), "service name cannot be null or empty");
        Assert.isTrue(SERVICE_NAME_PATTERN.matcher(service.getName()).matches(),
                () -> "service name must contain only lowercase letters, digits, and dashes: " + service.getName());
        Assert.notNull(service.getProperties(), "service properties cannot be null");
        Assert.notNull(service.getContainerType(), "service container type cannot be null");
        Assert.notNull(service.getContainerSupplier(), "service container supplier cannot be null");

        String containerBeanName = CONTAINER_BEAN_NAME_PREFIX + service.getName();
        String registrationBeanName = REGISTRATION_BEAN_NAME_PREFIX + service.getName();

        // 1. Ensure the container bean is not already defined. If it is, it means the application
        // has already started a container for this dev service, and the container is kept across
        // application restarts. Therefore, the registration bean is not needed, and the container
        // is not registered again.
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
        if (!containerKept && registerDiscoveredBeanDefinitions(service, registrationBeanName)) {
            return;
        }

        // 5. Register the container bean definition. If a container was kept across a DevTools
        // restart, the bean resolves to it. Otherwise, a new container is created and started,
        // meaning that the application owns its lifecycle.
        beanDefinitionRegistry.registerBeanDefinition(containerBeanName, createContainerBeanDefinition(service));

        // 6. Register the registration bean definition describing the dev service. It depends on
        // the container bean, so that the container is started before the dev service is reported
        // as ready.
        beanDefinitionRegistry.registerBeanDefinition(registrationBeanName, createRegistrationBeanDefinition(service, containerBeanName, containerKept));
    }

    private boolean isRegistrationBeanAlreadyDefined(String registrationBeanName) {
        return beanDefinitionRegistry.containsBeanDefinition(registrationBeanName);
    }

    private void validateContainerBeanDefinition(ServiceSpec service, String containerBeanName) {
        Assert.notNull(service.getContainerType(), "service container type cannot be null");
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
        ReuseStrategy configured = (service.getProperties() != null) ? service.getProperties().getReuseStrategy() : ReuseStrategy.NONE;
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
    private boolean registerDiscoveredBeanDefinitions(ServiceSpec service, String registrationBeanName) {
        Assert.hasText(service.getName(), "service name cannot be null or empty");

        Class<? extends ConnectionDetails> connectionDetailsType = service.getConnectionDetailsType();
        Function<DiscoveredContainer, ? extends ConnectionDetails> connectionDetailsFactory = service.getConnectionDetails();

        // 1. If not using the framework reuse strategy, no container is discovered.
        if (reuseDecision(service).effective() != ReuseStrategy.FRAMEWORK) {
            return false;
        }

        // 2. If no container is discovered, the application starts its own container.
        List<DiscoveredContainer> candidates = containerDiscovery.discover(service.getName());
        if (candidates.isEmpty()) {
            return false;
        }

        // 3. A ConnectionDetails bean of the declared type defined by the application takes
        // precedence over the one provided by the dev service, mirroring Spring Boot's service
        // connection behavior.
        Assert.notNull(connectionDetailsType, "connectionDetailsType cannot be null");
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
        Assert.notNull(connectionDetailsFactory, "connectionDetailsFactory cannot be null");
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
        RootBeanDefinition beanDefinition = new DevServiceConnectionDetailsBeanDefinition();
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
        Assert.hasText(service.getName(), "service name cannot be null or empty");

        String containerId = discoveredContainer.containerInfo().id();
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServiceRegistration.class);
        excludeFromAot(beanDefinition);
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        recordCategory(beanDefinition, service);

        beanDefinition.setInstanceSupplier(() -> {
            // Capture the links the discovered container exposes and log a consistent startup
            // message, so a dev service in a discovered container reports the same links as an owned one.
            List<DevServiceLink> links = DevServiceLinks.resolve(service, discoveredContainer.host(), discoveredContainer::mappedPort, false);
            DevServicesStartupLogger.ready(service.getName(), containerId, discoveredContainer.containerInfo().imageName(),
                    DevServiceRegistration.Origin.DISCOVERED, false, new ReuseDecision(ReuseStrategy.FRAMEWORK, null), links);

            return DevServiceRegistration.builder()
                    .name(service.getName())
                    .description(service.getDescription())
                    .origin(DevServiceRegistration.Origin.DISCOVERED)
                    .containerInfo(() -> ContainerRuntimeInfo.extractContainerInfoById(containerId))
                    .links(links)
                    .build();
        });

        return beanDefinition;
    }

    // OWNED

    private GenericBeanDefinition createContainerBeanDefinition(ServiceSpec service) {
        Supplier<? extends GenericContainer<?>> containerSupplier = service.getContainerSupplier();
        Assert.notNull(containerSupplier, "service container supplier cannot be null");

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
            applyCustomizers(container, registeredBean.getBeanFactory());
            applyLabels(container, service);
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

    private static void recordCategory(BeanDefinition beanDefinition, ServiceSpec service) {
        if (service.getCategory() != null) {
            beanDefinition.setAttribute(CATEGORY_ATTRIBUTE, service.getCategory());
        }
    }

    private void applyLabels(GenericContainer<?> container, ServiceSpec service) {
        container.withLabel(DevServiceLabels.NAME, service.getName());
        if (reuseDecision(service).effective() == ReuseStrategy.FRAMEWORK) {
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

    private RootBeanDefinition createRegistrationBeanDefinition(ServiceSpec service, String containerBeanName, boolean containerKept) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServiceRegistration.class);
        excludeFromAot(beanDefinition);
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        beanDefinition.setDependsOn(containerBeanName);
        recordCategory(beanDefinition, service);

        beanDefinition.setInstanceSupplier((InstanceSupplier<DevServiceRegistration>) registeredBean -> {
            GenericContainer<?> container = registeredBean.getBeanFactory().getBean(containerBeanName, GenericContainer.class);
            String containerId = container.getContainerId();

            // Capture the links the container exposes (the container is started at this point,
            // so mapped ports are available) and log a consistent startup message. The reuse
            // strategy is only part of it in dev mode, the only one where it applies.
            Assert.hasText(service.getName(), "service name cannot be null or empty");
            List<DevServiceLink> links = DevServiceLinks.resolve(service, container.getHost(), container::getMappedPort, true);
            String imageName = (container.getContainerInfo() != null) ? container.getContainerInfo().getConfig().getImage() : null;
            DevServicesStartupLogger.ready(service.getName(), containerId, imageName,
                    DevServiceRegistration.Origin.OWNED, containerKept, BootstrapMode.isDev() ? reuseDecision(service) : null, links);

            return DevServiceRegistration.builder()
                    .name(service.getName())
                    .description(service.getDescription())
                    .origin(DevServiceRegistration.Origin.OWNED)
                    .containerInfo(() -> ContainerRuntimeInfo.extractContainerInfoById(containerId))
                    .links(links)
                    .build();
        });

        return beanDefinition;
    }

}
