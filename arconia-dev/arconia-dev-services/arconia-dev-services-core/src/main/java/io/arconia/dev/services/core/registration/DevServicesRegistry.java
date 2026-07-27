package io.arconia.dev.services.core.registration;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.dockerjava.api.DockerClient;

import com.github.dockerjava.api.model.ContainerPort;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.TestcontainersConfiguration;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.core.support.Incubating;
import io.arconia.dev.services.api.provider.DevServiceProvider;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.autoconfigure.DevServicesConflictValidator;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;
import io.arconia.dev.services.core.container.DevServiceLabels;
import io.arconia.dev.services.core.container.StartupLogWaitStrategy;

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

    private static final String DEVTOOLS_RESTART_ENABLED_PROPERTY = "spring.devtools.restart.enabled";

    private static final String CONTAINER_BEAN_NAME_PREFIX = "devService.container.";

    private static final String CONNECTION_DETAILS_BEAN_NAME_PREFIX = "devService.connectionDetails.";

    private static final String REGISTRATION_BEAN_NAME_PREFIX = "devServiceRegistration.";

    /**
     * Prefix of the network alias Testcontainers automatically assigns to every
     * {@link GenericContainer} (e.g. {@code tc-a1b2c3d4}), used to tell auto-generated
     * aliases apart from user-defined ones.
     */
    private static final String GENERATED_NETWORK_ALIAS_PREFIX = "tc-";

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
     * <p>
     * When the service is shared, a running shared container started by another
     * application is used instead of starting a new container, if available.
     */
    private void registerBeanDefinition(ServiceSpec service) {
        Assert.hasText(service.getName(), "service name cannot be null or empty");
        Assert.notNull(service.getContainerSpec(), "service container cannot be null");
        Assert.notNull(service.getContainerSpec().getType(), "service container type cannot be null");
        Assert.notNull(service.getContainerSpec().getSupplier(), "service container supplier cannot be null");
        if (service.getSharingSpec() != null) {
            Assert.notNull(service.getSharingSpec().getConnectionDetailsType(), "connectionDetailsType cannot be null");
            Assert.notNull(service.getSharingSpec().getConnectionDetails(), "connectionDetails cannot be null");
        }

        // 1. Register the conflict validator bean that every dev service depends on,
        // so that mutually exclusive dev services fail fast before any container is created or adopted.
        registerConflictValidatorBeanDefinition();

        // 2. Use a shared container started by another application, if available.
        if (registerDiscoveredServiceBeanDefinitions(service)) {
            return;
        }

        // 3. Register the container bean
        String containerBeanName = CONTAINER_BEAN_NAME_PREFIX + service.getName();
        if (!beanDefinitionRegistry.containsBeanDefinition(containerBeanName)) {
            GenericBeanDefinition containerBeanDefinition = createContainerBeanDefinition(service);
            beanDefinitionRegistry.registerBeanDefinition(containerBeanName, containerBeanDefinition);
        }

        // 4. Register the description bean
        String descriptionBeanName = REGISTRATION_BEAN_NAME_PREFIX + service.getName();
        if (!this.beanDefinitionRegistry.containsBeanDefinition(descriptionBeanName)) {
            RootBeanDefinition descriptionBeanDefinition = createDescriptionBeanDefinition(service, containerBeanName);
            this.beanDefinitionRegistry.registerBeanDefinition(descriptionBeanName, descriptionBeanDefinition);
        }

    }

    /**
     * Register the beans for a shared dev service running in a container started by another
     * application, if any is available: the {@link ConnectionDetails} bean produced by the
     * service's sharing specification and the corresponding description bean. No container
     * bean is registered since the container lifecycle belongs to the owning application.
     * <p>
     * Discovery only applies in dev mode, and any failure results in falling back
     * to starting a dedicated container, never in breaking the application startup.
     *
     * @return whether a shared container was discovered and the service registered against it
     */
    private boolean registerDiscoveredServiceBeanDefinitions(ServiceSpec service) {
        SharingSpec sharingSpec = service.getSharingSpec();
        if (sharingSpec == null || !sharingSpec.isEnabled() || !isDevMode()) {
            return false;
        }

        // Sharing and reuse are mutually exclusive: a reused container outlives the application
        // and follows the Testcontainers reuse semantics, so it never joins the discovery ecosystem.
        if (sharingSpec.isReuse()) {
            logger.info("Sharing is disabled for the '{}' dev service because container reuse is enabled", service.getName());
            return false;
        }

        Class<? extends ConnectionDetails> connectionDetailsType = sharingSpec.getConnectionDetailsType();
        Assert.notNull(connectionDetailsType, "connectionDetailsType cannot be null");

        DiscoveredContainer discoveredContainer = findSharedContainer(service.getName());
        if (discoveredContainer == null) {
            return false;
        }
        ContainerInfo containerInfo = discoveredContainer.containerInfo();

        // A user-defined ConnectionDetails bean of the declared type takes precedence over the
        // dev-service-provided one, mirroring Spring Boot's service connections behavior.
        // The shared container is still adopted; only the connection details registration is skipped.
        String[] existingBeanNames = getExistingConnectionDetailsBeanNames(connectionDetailsType);
        if (existingBeanNames.length == 0) {
            ConnectionDetails connectionDetails;
            try {
                connectionDetails = sharingSpec.getConnectionDetails().apply(discoveredContainer);
                Assert.notNull(connectionDetails, "connectionDetails cannot be null");
                Assert.state(connectionDetailsType.isInstance(connectionDetails), "connectionDetails must be an instance of " + connectionDetailsType.getName());
            } catch (Exception ex) {
                // A malformed adoption candidate (e.g. a container not publishing the expected port)
                // must never break the application startup.
                logger.warn("Failed to build the connection details for the shared '{}' dev service in container {}. Starting a dedicated container instead.",
                        service.getName(), containerInfo.id(), ex);
                return false;
            }

            String connectionDetailsBeanName = CONNECTION_DETAILS_BEAN_NAME_PREFIX + service.getName();
            if (!beanDefinitionRegistry.containsBeanDefinition(connectionDetailsBeanName)) {
                beanDefinitionRegistry.registerBeanDefinition(connectionDetailsBeanName,
                        createDiscoveredConnectionDetailsBeanDefinition(connectionDetails, containerInfo));
            }
        } else {
            logger.debug("Skipping the connection details registration for the shared '{}' dev service due to existing beans {}",
                    service.getName(), Arrays.asList(existingBeanNames));
        }

        String descriptionBeanName = REGISTRATION_BEAN_NAME_PREFIX + service.getName();
        if (!beanDefinitionRegistry.containsBeanDefinition(descriptionBeanName)) {
            beanDefinitionRegistry.registerBeanDefinition(descriptionBeanName,
                    createDiscoveredDescriptionBeanDefinition(service, containerInfo.id()));
        }

        logger.info("Using shared '{}' dev service from container {} started by another application",
                service.getName(), containerInfo.id());
        return true;
    }

    /**
     * Find a running container providing a shared dev service with the given name,
     * started by another application. The lookup is label-based and config-agnostic:
     * the container is adopted as it runs, with the owning application's configuration.
     *
     * @return the discovered container, or {@code null} when no shared container
     * is available or the container runtime cannot be queried
     */
    @Nullable
    DiscoveredContainer findSharedContainer(String serviceName) {
        try {
            // Get Docker client from Testcontainers. We don't close the connection as it's handled
            // globally by the DockerClientFactory.
            DockerClient dockerClient = DockerClientFactory.lazyClient();
            return dockerClient.listContainersCmd()
                    .withLabelFilter(Map.of(DevServiceLabels.NAME, serviceName, DevServiceLabels.SHARED, "true"))
                    // Paused or restarting containers are never valid candidates.
                    .withStatusFilter(List.of("running"))
                    .exec()
                    .stream()
                    .filter(dockerContainer -> dockerContainer.getLabels() == null
                            || !DevServiceLabels.ownerId().equals(dockerContainer.getLabels().get(DevServiceLabels.OWNER)))
                    // Pick the oldest candidate so that applications starting concurrently
                    // converge deterministically on the same container. Creation timestamps
                    // have second granularity, so ties are broken by container ID.
                    .min(Comparator.<com.github.dockerjava.api.model.Container>comparingLong(dockerContainer ->
                            dockerContainer.getCreated() != null ? dockerContainer.getCreated() : Long.MAX_VALUE)
                            .thenComparing(dockerContainer ->
                                    dockerContainer.getId() != null ? dockerContainer.getId() : ""))
                    .map(dockerContainer -> new DiscoveredContainer(toContainerInfo(dockerContainer),
                            DockerClientFactory.instance().dockerHostIpAddress()))
                    .orElse(null);
        } catch (Exception ex) {
            logger.info("Failed to look up shared containers for the '{}' dev service. Starting a dedicated container instead.", serviceName, ex);
            return null;
        }
    }

    /**
     * The names of the existing beans of the given connection details type, if any.
     */
    private String[] getExistingConnectionDetailsBeanNames(Class<?> connectionDetailsType) {
        if (beanDefinitionRegistry instanceof ListableBeanFactory listableBeanFactory) {
            return listableBeanFactory.getBeanNamesForType(connectionDetailsType);
        }
        return new String[0];
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
                applyLabels(container, service);
                applyNetwork(container, service, registeredBean.getBeanFactory());
                applyStartupLogging(container, service);
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
     * Apply the dev service labels to the given container, making it identifiable
     * and, when sharing is enabled, discoverable by other applications.
     * <p>
     * A container configured for reuse is never advertised as shared (sharing and reuse
     * are mutually exclusive), and its owner label is omitted when the environment actually
     * supports reuse, since user labels contribute to the Testcontainers reuse hash, and
     * a per-JVM value would prevent reusing the container across applications and restarts.
     */
    private void applyLabels(Container<?> container, ServiceSpec service) {
        if (!(container instanceof GenericContainer<?> genericContainer)) {
            return;
        }
        boolean reuse = genericContainer.isShouldBeReused();
        genericContainer.withLabel(DevServiceLabels.NAME, service.getName());
        genericContainer.withLabel(DevServiceLabels.SHARED, String.valueOf(!reuse && isSharingEnabled(service)));
        if (!(reuse && environmentSupportsReuse())) {
            genericContainer.withLabel(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
        }
    }

    /**
     * Attach the given container to the shared dev services {@link Network} when the service
     * opts in, so it can communicate with other dev service containers.
     * <p>
     * Containers are reachable by the aliases set via the {@code network-aliases} property
     * (already applied to the container). When none is set, the service name is applied as a
     * default alias, so other containers always have a stable, predictable name to reach it by;
     * a pure consumer that is never reached simply leaves that alias unused, which is harmless.
     * A network already set on the container (e.g. by a customizer for a multi-container service)
     * is honored. Port mapping is left untouched: the application keeps reaching the container
     * over the host and mapped ports via its connection details.
     */
    private void applyNetwork(Container<?> container, ServiceSpec service, ConfigurableBeanFactory beanFactory) {
        NetworkSpec networkSpec = service.getNetworkSpec();
        if (networkSpec == null || !networkSpec.isEnabled()) {
            return;
        }
        if (!(container instanceof GenericContainer<?> genericContainer)) {
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
        // are part of the Testcontainers reuse hash (which is why applyLabels omits the per-JVM
        // owner label under reuse), so a random alias would defeat reuse. Note the reuse hash also
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
        // Record the stable aliases in a label so discovery and (future) auto-wiring can reach the
        // container by name without assuming any particular naming convention.
        genericContainer.withLabel(DevServiceLabels.NETWORK_ALIASES, String.join(",", aliases));

        if (genericContainer.isShouldBeReused() && network == Network.SHARED) {
            logger.warn("Container reuse is ineffective for the networked '{}' dev service on the default isolated network; set 'arconia.dev.services.network.name' for a stable network", service.getName());
        }
    }

    /**
     * Wrap the container's wait strategy so that when the container fails to start, its logs are
     * written to the application log (at the configured level).
     */
    private void applyStartupLogging(Container<?> container, ServiceSpec service) {
        if (!(container instanceof GenericContainer<?> genericContainer)) {
            return;
        }
        // GenericContainer#getWaitStrategy() is protected, so read the current strategy from the
        // (protected) field to wrap it. Skip gracefully if it can't be read, so the container still
        // starts normally, just without the on-failure log dump.
        WaitStrategy current = currentWaitStrategy(genericContainer);
        if (current == null) {
            return;
        }
        genericContainer.setWaitStrategy(new StartupLogWaitStrategy(current, service.getName(), resolveStartupLogLevel()));
    }

    @Nullable
    private static WaitStrategy currentWaitStrategy(GenericContainer<?> container) {
        try {
            Field field = ReflectionUtils.findField(GenericContainer.class, "waitStrategy");
            if (field == null) {
                return null;
            }
            ReflectionUtils.makeAccessible(field);
            return (ReflectionUtils.getField(field, container) instanceof WaitStrategy waitStrategy) ? waitStrategy : null;
        } catch (Exception ex) {
            logger.debug("Failed to read the wait strategy of the '{}' dev service container; on-failure log capture is disabled for it", container.getDockerImageName(), ex);
            return null;
        }
    }

    /**
     * The level at which a failing dev service container's logs are written to the application log.
     */
    private LogLevel resolveStartupLogLevel() {
        return Binder.get(environment)
                .bind("arconia.dev.services.startup.log-level", LogLevel.class)
                .orElse(LogLevel.INFO);
    }

    /**
     * The links a container exposes (management consoles, telemetry endpoints, …) for display in
     * startup logs and developer tooling, or an empty list when the container declares none.
     */
    private static List<DevServiceLink> resolveLinks(Container<?> container) {
        if (container instanceof DevServiceLinkProvider linkProvider) {
            try {
                List<DevServiceLink> links = linkProvider.devServiceLinks();
                return (links != null) ? links : List.of();
            } catch (Exception ex) {
                logger.debug("Failed to resolve links for a dev service container", ex);
            }
        }
        return List.of();
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

            // Capture the links the container exposes (the container is started at this point,
            // so mapped ports are available) and log a consistent startup message.
            List<DevServiceLink> links = resolveLinks(container);
            DevServicesStartupLogger.owned(service.getName(), links);

            return DevServiceRegistration.builder()
                    .name(service.getName())
                    .description(service.getDescription())
                    .origin(DevServiceRegistration.Origin.OWNED)
                    .containerInfo(containerInfoSupplier)
                    .links(links)
                    .build();

        });

        return descriptionBeanDefinition;
    }

    /**
     * Create the bean definition for the {@link ConnectionDetails} of a discovered shared dev service.
     * The bean takes the place of the container-based connection details that Spring Boot would
     * produce for an owned container via the {@code @ServiceConnection} mechanism.
     */
    private RootBeanDefinition createDiscoveredConnectionDetailsBeanDefinition(ConnectionDetails connectionDetails, ContainerInfo containerInfo) {
        RootBeanDefinition beanDefinition = new DevServiceConnectionDetailsBeanDefinition();
        beanDefinition.setBeanClass(connectionDetails.getClass());
        beanDefinition.setInstanceSupplier(() -> connectionDetails);

        // Attach the container image metadata, letting downstream auto-configurations
        // introspect which image backs the connection details bean.
        new ContainerImageMetadata(containerInfo.imageName()).addTo(beanDefinition);

        // Hint that this bean has an infrastructure role, meaning it has no relevance to the end-user.
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        // Ensure mutually exclusive dev services are validated before the service is wired.
        // Discovered services have no container bean, so this bean carries the dependency instead.
        beanDefinition.setDependsOn(CONFLICT_VALIDATOR_BEAN_NAME);

        return beanDefinition;
    }

    /**
     * Create the description bean definition for a discovered shared dev service.
     * Unlike the owned variant, the container ID comes straight from the discovery query,
     * so the bean doesn't depend on any container bean.
     */
    private RootBeanDefinition createDiscoveredDescriptionBeanDefinition(ServiceSpec service, String containerId) {
        RootBeanDefinition descriptionBeanDefinition = new RootBeanDefinition();
        descriptionBeanDefinition.setBeanClass(DevServiceRegistration.class);

        // Hint that this bean has a support role, meaning it is a supporting part of some larger configuration.
        descriptionBeanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);

        descriptionBeanDefinition.setDependsOn(CONFLICT_VALIDATOR_BEAN_NAME);

        descriptionBeanDefinition.setInstanceSupplier(() -> {
            DevServicesStartupLogger.discovered(service.getName(), List.of());
            return DevServiceRegistration.builder()
                    .name(service.getName())
                    .description(service.getDescription())
                    .origin(DevServiceRegistration.Origin.DISCOVERED)
                    .containerInfo(() -> extractContainerInfoById(containerId))
                    .build();
        });

        return descriptionBeanDefinition;
    }

    /**
     * Whether the environment supports the Testcontainers reusable containers feature.
     * When it doesn't, a container configured for reuse is started as an ordinary container,
     * so the owner label is kept to preserve the protection against self-discovery.
     */
    boolean environmentSupportsReuse() {
        return TestcontainersConfiguration.getInstance().environmentSupportsReuse();
    }

    /**
     * Whether the dev service is shared among applications: the service declares
     * a sharing specification, sharing is enabled for it, and the application runs in dev mode.
     */
    private static boolean isSharingEnabled(ServiceSpec service) {
        return service.getSharingSpec() != null && service.getSharingSpec().isEnabled() && isDevMode();
    }

    private static boolean isDevMode() {
        return BootstrapMode.DEV.equals(BootstrapMode.detect());
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

            return toContainerInfo(dockerContainer);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to extract container information for ID: " + containerId, ex);
        }
    }

    /**
     * Map container details from the OCI runtime API to a {@link ContainerInfo}.
     */
    private static ContainerInfo toContainerInfo(com.github.dockerjava.api.model.Container dockerContainer) {
        List<String> names = Arrays.stream(dockerContainer.getNames() != null ? dockerContainer.getNames() : new String[0])
                .map(name -> name.startsWith("/") ? name.substring(1) : name)
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

        return new ContainerInfo(dockerContainer.getId(), imageName, names, exposedPorts, labels, status);
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

        @Nullable
        private SharingSpec sharingSpec;

        @Nullable
        private NetworkSpec networkSpec;

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

        /**
         * Specification for sharing the dev service among applications.
         * A dev service that declares no sharing specification never participates
         * in discovery, even when the {@code shared} property is enabled.
         */
        public ServiceSpec sharing(Consumer<SharingSpec> sharingSpecConsumer) {
            var sharingSpec = new SharingSpec();
            sharingSpecConsumer.accept(sharingSpec);
            this.sharingSpec = sharingSpec;
            return this;
        }

        /**
         * Specification for joining the shared dev services network, so the container
         * can communicate with other dev service containers. A dev service that declares
         * no network specification never joins a network.
         */
        public ServiceSpec network(Consumer<NetworkSpec> networkSpecConsumer) {
            var networkSpec = new NetworkSpec();
            networkSpecConsumer.accept(networkSpec);
            this.networkSpec = networkSpec;
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

        @Nullable
        SharingSpec getSharingSpec() {
            return sharingSpec;
        }

        @Nullable
        NetworkSpec getNetworkSpec() {
            return networkSpec;
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

    /**
     * Specification for sharing a dev service among applications.
     * <p>
     * When sharing is enabled, the dev service container is discoverable by other
     * applications, and the application connects to an existing shared container
     * (adopted as it runs, with the owning application's configuration)
     * if available instead of starting a new one.
     */
    public static final class SharingSpec {

        private boolean enabled = false;

        private boolean reuse = false;

        @Nullable
        private Class<? extends ConnectionDetails> connectionDetailsType;

        @Nullable
        private Function<DiscoveredContainer, ? extends ConnectionDetails> connectionDetails;

        private SharingSpec() {}

        /**
         * Whether sharing is enabled for the dev service,
         * typically bound to the {@code shared} configuration property.
         */
        public SharingSpec enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Whether container reuse is enabled for the dev service,
         * typically bound to the {@code reuse} configuration property.
         * Sharing and reuse are mutually exclusive: when reuse is enabled,
         * sharing is disabled for the dev service.
         */
        public SharingSpec reuse(boolean reuse) {
            this.reuse = reuse;
            return this;
        }

        /**
         * The type of {@link ConnectionDetails} provided for the dev service and a factory
         * function providing the instance for connecting to a shared dev service discovered
         * in a running container.
         * <p>
         * The declared type is used to look up existing user-defined {@code ConnectionDetails}
         * beans: when one is present, it takes precedence over the one provided by the dev service.
         */
        public <T extends ConnectionDetails> SharingSpec connectionDetails(Class<T> connectionDetailsType,
                Function<DiscoveredContainer, ? extends T> connectionDetails) {
            Assert.state(this.connectionDetails == null, "sharing supports a single connection details contribution");
            this.connectionDetailsType = connectionDetailsType;
            this.connectionDetails = connectionDetails;
            return this;
        }

        boolean isEnabled() {
            return enabled;
        }

        boolean isReuse() {
            return reuse;
        }

        @Nullable
        Class<? extends ConnectionDetails> getConnectionDetailsType() {
            return connectionDetailsType;
        }

        @Nullable
        Function<DiscoveredContainer, ? extends ConnectionDetails> getConnectionDetails() {
            return connectionDetails;
        }

    }

    /**
     * Specification for joining the shared dev services network.
     * <p>
     * When enabled, the dev service container is attached to the injectable {@link Network}
     * bean, so it can communicate with other dev service containers over an OCI network.
     * This is distinct from sharing, which shares the same container across applications.
     */
    public static final class NetworkSpec {

        private boolean enabled = false;

        private NetworkSpec() {}

        /**
         * Whether the dev service joins the shared dev services network,
         * typically bound to the {@code join-network} configuration property.
         */
        public NetworkSpec enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        boolean isEnabled() {
            return enabled;
        }

    }

}
