package io.arconia.dev.services.core.registration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.InstanceSupplier;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
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
import io.arconia.dev.services.api.provider.DevServiceProvider;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.autoconfigure.DevServicesConflictValidator;
import io.arconia.dev.services.core.autoconfigure.DevServicesProperties;
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
     * Prefix of the network alias Testcontainers automatically assigns to every
     * {@link GenericContainer} (e.g. {@code tc-a1b2c3d4}), used to tell auto-generated
     * aliases apart from user-defined ones.
     */
    private static final String GENERATED_NETWORK_ALIAS_PREFIX = "tc-";

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

        // 2. Ensure the conflict validation bean is defined before the container bean is registered,
        // so that the conflict is detected before the container is created.
        registerConflictValidatorBeanDefinition();

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
        beanDefinitionRegistry.registerBeanDefinition(registrationBeanName, createRegistrationBeanDefinition(service, containerBeanName));
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

    private void registerConflictValidatorBeanDefinition() {
        if (beanDefinitionRegistry.containsBeanDefinition(CONFLICT_VALIDATOR_BEAN_NAME)) {
            return;
        }

        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServicesConflictValidator.class);
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        beanDefinition.setInstanceSupplier((InstanceSupplier<DevServicesConflictValidator>) registeredBean -> {
            DevServicesConflictValidator validator = new DevServicesConflictValidator();
            ConfigurableListableBeanFactory listableBeanFactory = registeredBean.getBeanFactory();
            validator.validate(listableBeanFactory.getBeansOfType(DevServiceProvider.class).values());
            return validator;
        });

        beanDefinitionRegistry.registerBeanDefinition(CONFLICT_VALIDATOR_BEAN_NAME, beanDefinition);
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
                        service.getName(), DevServicesStartupLogger.computeContainerShortId(candidate.containerInfo().id()), ex);
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
        beanDefinition.setBeanClass(connectionDetails.getClass());
        beanDefinition.setInstanceSupplier(() -> connectionDetails);
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        beanDefinition.setDependsOn(CONFLICT_VALIDATOR_BEAN_NAME);

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
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        beanDefinition.setDependsOn(CONFLICT_VALIDATOR_BEAN_NAME);

        beanDefinition.setInstanceSupplier(() -> {
            // Capture the links the discovered container exposes and log a consistent startup
            // message, so a dev service in a discovered container reports the same links as an owned one.
            List<DevServiceLink> links = DevServiceLinks.resolve(discoveredContainer);
            DevServicesStartupLogger.discovered(service.getName(), containerId, links);

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

        // Ensure mutually exclusive dev services are validated before the container is created.
        beanDefinition.setDependsOn(CONFLICT_VALIDATOR_BEAN_NAME);

        return beanDefinition;
    }

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
        if (beanName != null) {
            // containsBeanDefinition is local-only, so a customizer registered as a lambda @Bean in a
            // parent context isn't found here; walk the parent chain to the factory that owns the
            // definition and read its type before falling back to the customizer's class.
            ConfigurableListableBeanFactory definingBeanFactory = findDefiningBeanFactory(beanFactory, beanName);
            if (definingBeanFactory != null) {
                customizerType = definingBeanFactory.getMergedBeanDefinition(beanName).getResolvableType();
            }
        }

        Class<?> targetType = customizerType.as(DevServiceContainerCustomizer.class).getGeneric(0).resolve();
        if (targetType == null) {
            targetType = GenericTypeResolver.resolveTypeArgument(customizer.getClass(), DevServiceContainerCustomizer.class);
        }

        return targetType == null || targetType.isInstance(container);
    }

    /**
     * Find the bean factory in the hierarchy that owns the definition of the given bean.
     * {@code containsBeanDefinition} is local to a single factory, so the parent chain is
     * walked to locate a customizer registered in a parent context.
     */
    @Nullable
    private static ConfigurableListableBeanFactory findDefiningBeanFactory(ConfigurableListableBeanFactory beanFactory, String beanName) {
        BeanFactory current = beanFactory;
        while (current instanceof ConfigurableListableBeanFactory listableBeanFactory) {
            if (listableBeanFactory.containsBeanDefinition(beanName)) {
                return listableBeanFactory;
            }
            current = listableBeanFactory.getParentBeanFactory();
        }
        return null;
    }

    private void applyLabels(GenericContainer<?> container, ServiceSpec service) {
        container.withLabel(DevServiceLabels.NAME, service.getName());
        // Record the links the container exposes so that an application discovering it
        // reports the same links, without having to declare them a second time.
        // Ports are recorded as the container sees them, since no port is mapped yet.
        // Like every other label, these take part in the Testcontainers reuse hash: they are
        // stable across runs, so reuse keeps working, but changing a link changes the hash and
        // the next run starts a new container instead of reusing the previous one.
        for (DevServiceLinkDefinition link : DevServiceLinks.declaredBy(container)) {
            DevServiceLabels.linkLabels(link).forEach(container::withLabel);
        }
        if (reuseDecision(service).effective() == ReuseStrategy.FRAMEWORK) {
            container.withLabel(DevServiceLabels.DISCOVERABLE, "true");
            container.withLabel(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
        }
    }

    /**
     * Attach the given container to the shared dev services {@link Network} when the global
     * {@code arconia.dev.services.network.enabled} switch is on, so all dev service containers
     * can communicate with each other. Networking is global rather than per-service: the set of
     * containers that need to reach each other is defined by the user's topology, not by any
     * single dev service, so enabling the switch attaches every container.
     * <p>
     * Containers are reachable by the aliases set via the {@code network-aliases} property
     * (already applied to the container). When none is set, the service name is applied as a
     * default alias, so other containers always have a stable, predictable name to reach it by;
     * a pure consumer that is never reached simply leaves that alias unused, which is harmless.
     * A network already set on the container (e.g. by a customizer for a multi-container service)
     * is honored. Port mapping is left untouched: the application keeps reaching the container
     * over the host and mapped ports via its connection details.
     */
    private void applyNetwork(GenericContainer<?> genericContainer, ServiceSpec service, ConfigurableBeanFactory beanFactory) {
        if (!isNetworkEnabled()) {
            return;
        }

        Network network = beanFactory.getBeanProvider(Network.class).getIfUnique();
        if (network == null) {
            logger.warn("The '{}' dev service is configured to join a network, but no unique Network bean is available (none or more than one is defined); skipping network attachment", service.getName());
            return;
        }

        // Honor a network already set on the container (e.g. by a customizer).
        if (genericContainer.getNetwork() == null) {
            genericContainer.withNetwork(network);
        }

        // Testcontainers always seeds a generated "tc-<random>" alias, so a container is never
        // fully unreachable; but that alias is unpredictable. Prefer user-defined aliases (set via
        // the "network-aliases" property); when none is set, default to the service name so other
        // containers have a stable, predictable name to reach it by. Both are stable across runs,
        // which keeps the label (recorded below) reuse-safe: container labels and network aliases
        // are part of the Testcontainers reuse hash, so a random alias would defeat reuse.
        // Note the reuse hash also
        // includes the network id: it is stable for a named network but per-JVM for Network.SHARED,
        // which is what the reuse warning below is about.
        List<String> userAliases = genericContainer.getNetworkAliases().stream()
                .filter(alias -> !alias.startsWith(GENERATED_NETWORK_ALIAS_PREFIX))
                .toList();
        List<String> aliases;
        if (userAliases.isEmpty()) {
            genericContainer.withNetworkAliases(service.getName());
            aliases = List.of(service.getName());
        } else {
            aliases = userAliases;
        }
        // Record the stable aliases in a label, so that the name the container is
        // reachable by on the network can be read from the container itself.
        genericContainer.withLabel(DevServiceLabels.NETWORK_ALIASES, String.join(",", aliases));

        if (genericContainer.isShouldBeReused() && network == Network.SHARED) {
            logger.warn("The 'testcontainers' reuse strategy is ineffective for the '{}' dev service on the default isolated network; set 'arconia.dev.services.network.name' for a stable network", service.getName());
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

    private RootBeanDefinition createRegistrationBeanDefinition(ServiceSpec service, String containerBeanName) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServiceRegistration.class);
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        beanDefinition.setDependsOn(containerBeanName);

        beanDefinition.setInstanceSupplier((InstanceSupplier<DevServiceRegistration>) registeredBean -> {
            GenericContainer<?> container = registeredBean.getBeanFactory().getBean(containerBeanName, GenericContainer.class);
            String containerId = container.getContainerId();

            // Capture the links the container exposes (the container is started at this point,
            // so mapped ports are available) and log a consistent startup message. The reuse
            // strategy is only part of it in dev mode, the only one where it applies.
            Assert.hasText(service.getName(), "service name cannot be null or empty");
            List<DevServiceLink> links = DevServiceLinks.resolve(container);
            DevServicesStartupLogger.owned(service.getName(), containerId,
                    BootstrapMode.isDev() ? reuseDecision(service) : null, links);

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
