package io.arconia.dev.services.core.registration;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
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
import org.springframework.boot.autoconfigure.ssl.SslBundleRegistrar;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
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
import io.arconia.dev.services.core.registration.DevServiceDefinition.Discovery;
import io.arconia.dev.services.core.registration.DevServiceDefinition.PortBinding;
import io.arconia.dev.services.core.registration.DevServiceSslSupport.DevServiceSslBundle;

/**
 * Registry for managing the definition and lifecycle of dev services.
 */
@Incubating
public final class DevServicesRegistry {

    private static final Logger logger = LoggerFactory.getLogger(DevServicesRegistry.class);

    private final BeanDefinitionRegistry beanDefinitionRegistry;

    private final ResourceLoader resourceLoader;

    private final ContainerDiscovery containerDiscovery;

    private final BooleanSupplier testcontainersReuseSupport;

    private final DevServiceRestartSupport restartSupport;

    private final DevServiceNetworkSupport networkSupport;

    DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment, ResourceLoader resourceLoader) {
        this(beanDefinitionRegistry, environment, resourceLoader, new DockerContainerDiscovery(),
                () -> TestcontainersConfiguration.getInstance().environmentSupportsReuse());
    }

    DevServicesRegistry(BeanDefinitionRegistry beanDefinitionRegistry, Environment environment, ResourceLoader resourceLoader,
            ContainerDiscovery containerDiscovery, BooleanSupplier testcontainersReuseSupport) {
        Assert.notNull(beanDefinitionRegistry, "beanDefinitionRegistry cannot be null");
        Assert.notNull(environment, "environment cannot be null");
        Assert.notNull(resourceLoader, "resourceLoader cannot be null");
        Assert.notNull(containerDiscovery, "containerDiscovery cannot be null");
        Assert.notNull(testcontainersReuseSupport, "testcontainersReuseSupport cannot be null");
        this.beanDefinitionRegistry = beanDefinitionRegistry;
        this.resourceLoader = resourceLoader;
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
        Assert.notNull(service, "service cannot be null");
        DevServiceSpec serviceSpec = new DevServiceSpec();
        service.accept(serviceSpec);
        registerBeanDefinitions(serviceSpec.build());
    }

    /**
     * Register all the beans associated with a dev service.
     */
    private void registerBeanDefinitions(DevServiceDefinition service) {
        ReuseDecision reuse = ReuseDecision.decide(service, testcontainersReuseSupport, networkSupport.isEnabled());

        String containerBeanName = BeanName.CONTAINER.of(service.name());
        String registrationBeanName = BeanName.REGISTRATION.of(service.name());

        // 1. A dev service is registered once per application:
        // registering another one with the same name is an error.
        Assert.state(!beanDefinitionRegistry.containsBeanDefinition(registrationBeanName),
                () -> "A dev service named '%s' is already registered".formatted(service.name()));

        // 2. A disabled dev service is not started nor registered. The auto-configuration
        // condition normally backs off earlier; this honors the properties contract for a dev
        // service whose condition doesn't read the property. Neither is a dev service that is
        // skipped in test mode, such as an observability platform. A ConnectionDetails bean of the
        // declared type defined by the application takes precedence over the dev service as well,
        // mirroring Spring Boot's service connection behavior: the application connects to the
        // service of its choice. Either way, a container kept across a DevTools restart is no
        // longer needed. A skipped dev service takes no part in the mutual exclusion below.
        if (isDisabled(service) || isSkippedInTests(service) || isConnectionDetailsDefinedByApplication(service)) {
            restartSupport.discardKeptContainer(service.name(), containerBeanName);
            return;
        }

        // 3. Ensure no other dev service of the same category is registered, since dev services
        // providing the same capability are mutually exclusive.
        ensureCategoryIsFree(service);

        // 4. Check whether the application has kept a running container for this dev service across a
        // DevTools restart. If it has, the application keeps using it.
        boolean containerKept = restartSupport.retainKeptContainer(service.name(), containerBeanName, service.containerType());

        // 5. A dev service with SSL enabled gets its SSL bundle built from its properties, whether
        // it starts a container or connects to a discovered one. The bundle the application
        // connects with, which trusts the certificate the dev service presents, is registered in
        // the SslBundles of the application, for its tooling to report it and for it to be
        // referenced by name.
        DevServiceSslBundle sslBundle = DevServiceSslSupport.createBundle(service, resourceLoader);
        SslBundle trustBundle = (sslBundle != null) ? sslBundle.trustBundle() : null;
        if (trustBundle != null) {
            beanDefinitionRegistry.registerBeanDefinition(BeanName.SSL_BUNDLE.of(service.name()), createSslBundleBeanDefinition(service, trustBundle));
        }

        // 6. If no container was kept, look for a container started by another application, when
        // the framework reuse strategy applies. If one is discovered, the beans for it are
        // registered instead, and the application does not start a container of its own.
        if (!containerKept && registerDiscoveredBeanDefinitions(service, reuse, registrationBeanName, trustBundle)) {
            return;
        }

        // 7. Register the container bean definition. If a container was kept across a DevTools
        // restart, the bean resolves to it. Otherwise, a new container is created and started,
        // meaning that the application manages its lifecycle.
        beanDefinitionRegistry.registerBeanDefinition(containerBeanName, createContainerBeanDefinition(service, reuse, sslBundle));

        // 8. Register the registration bean definition describing the dev service. It depends on
        // the container bean, so that the container is started before the dev service is reported
        // as ready.
        beanDefinitionRegistry.registerBeanDefinition(registrationBeanName, createRegistrationBeanDefinition(service, reuse, containerBeanName, containerKept, sslBundle != null));
    }

    /**
     * Create the bean definition registering the SSL bundle the application connects to a dev
     * service with in the {@link org.springframework.boot.ssl.SslBundles} of the application,
     * under the name the dev service reports it by.
     */
    private static RootBeanDefinition createSslBundleBeanDefinition(DevServiceDefinition service, SslBundle trustBundle) {
        String bundleName = DevServiceSslSupport.bundleName(service.name());
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        BeanAttribute.AOT_EXCLUDED.set(beanDefinition, true);
        beanDefinition.setBeanClass(SslBundleRegistrar.class);
        beanDefinition.setInstanceSupplier(() -> (SslBundleRegistrar) registry -> registry.registerBundle(bundleName, trustBundle));
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        return beanDefinition;
    }

    /**
     * Fail if a dev service of the same category as the given one is already registered.
     * The category of a registered dev service is recorded on its registration bean definition.
     */
    private void ensureCategoryIsFree(DevServiceDefinition service) {
        String category = service.category();
        if (category == null) {
            return;
        }
        for (String beanName : beanDefinitionRegistry.getBeanDefinitionNames()) {
            if (BeanName.REGISTRATION.matches(beanName)
                    && category.equals(BeanAttribute.CATEGORY.get(beanDefinitionRegistry.getBeanDefinition(beanName)))) {
                String otherName = BeanName.REGISTRATION.serviceName(beanName);
                throw new MultipleDevServicesException(category, Stream.of(otherName, service.name()).sorted().toList());
            }
        }
    }

    /**
     * Whether the dev service is disabled through its properties.
     */
    private static boolean isDisabled(DevServiceDefinition service) {
        if (service.properties().isEnabled()) {
            return false;
        }
        logger.info("Dev Service '{}' is disabled", service.name());
        return true;
    }

    /**
     * Whether the dev service is skipped in test mode through its properties.
     */
    private static boolean isSkippedInTests(DevServiceDefinition service) {
        if (!BootstrapMode.isTest() || !service.properties().isSkipInTests()) {
            return false;
        }
        logger.info("Dev Service '{}' is skipped in test mode", service.name());
        return true;
    }

    /**
     * Whether the application defines a {@link ConnectionDetails} bean of the type the dev
     * service declares. Only a dev service declaring its discovery states that type.
     */
    private boolean isConnectionDetailsDefinedByApplication(DevServiceDefinition service) {
        Discovery discovery = service.discovery();
        if (discovery == null || !(beanDefinitionRegistry instanceof ListableBeanFactory listableBeanFactory)) {
            return false;
        }
        String[] existingBeanNames = listableBeanFactory.getBeanNamesForType(discovery.type(), true, false);
        if (existingBeanNames.length == 0) {
            return false;
        }
        logger.info("Dev Service '{}' is not started: a {} bean already exists in the application ({})",
                service.name(), discovery.type().getSimpleName(), String.join(", ", existingBeanNames));
        return true;
    }

    // DISCOVERED

    /**
     * Register the beans associated with a dev service running in a container started by another
     * application, if one is discovered, and returns whether that happened.
     */
    private boolean registerDiscoveredBeanDefinitions(DevServiceDefinition service, ReuseDecision reuse, String registrationBeanName,
            @Nullable SslBundle trustBundle) {
        // 1. Containers are only discovered under the framework reuse strategy, which is only
        // in effect when the dev service declares its discovery.
        Discovery discovery = service.discovery();
        if (reuse.effective() != ReuseStrategy.FRAMEWORK || discovery == null) {
            return false;
        }

        // 2. If no container is discovered, the application starts its own container.
        List<DiscoveredContainer> candidates = containerDiscovery.discover(service.name());
        if (candidates.isEmpty()) {
            return false;
        }

        // 3. Register a ConnectionDetails bean for each discovered container and a bean describing the dev service.
        // Candidates are processed from oldest to newest. If a container cannot be connected to, the candidate is skipped.
        // A discovered container of a dev service with SSL enabled is connected to with the bundle
        // trusting the certificate the dev service is configured to present, as a managed one would be.
        for (DiscoveredContainer discovered : candidates) {
            DiscoveredContainer candidate = DiscoveredContainer.builder()
                    .containerInfo(discovered.containerInfo())
                    .host(discovered.host())
                    .sslBundle(trustBundle)
                    .build();
            try {
                ConnectionDetails connectionDetails = discovery.factory().apply(candidate);
                Assert.state(discovery.type().isInstance(connectionDetails), "connectionDetails must be an instance of " + discovery.type().getName());
                beanDefinitionRegistry.registerBeanDefinition(BeanName.CONNECTION_DETAILS.of(service.name()),
                        createDiscoveredConnectionDetailsBeanDefinition(connectionDetails, candidate.containerInfo()));
                beanDefinitionRegistry.registerBeanDefinition(registrationBeanName,
                        createDiscoveredRegistrationBeanDefinition(service, reuse, candidate, trustBundle != null));
                return true;
            } catch (Exception ex) {
                logger.warn("Dev Service '{}': failed to build the connection details for the discovered container {}. Skipping it.",
                        service.name(), ContainerRuntimeInfo.shortId(candidate.containerInfo().getId()), ex);
            }
        }

        logger.info("Dev Service '{}': no discovered container can be used. Starting its own container instead.", service.name());
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
        new ContainerImageMetadata(containerInfo.getImageName()).addTo(beanDefinition);

        return beanDefinition;
    }

    /**
     * Create the bean definition describing a dev service running in a discovered container.
     * Unlike the managed variant, the container ID comes straight from the discovery,
     * so the bean doesn't depend on any container bean.
     */
    private static RootBeanDefinition createDiscoveredRegistrationBeanDefinition(DevServiceDefinition service, ReuseDecision reuse,
            DiscoveredContainer discoveredContainer, boolean sslBundleRegistered) {
        RootBeanDefinition beanDefinition = newRegistrationBeanDefinition(service);

        ContainerInfo containerInfo = discoveredContainer.containerInfo();
        beanDefinition.setInstanceSupplier(() -> {
            // The links are resolved against the ports the discovered container publishes.
            List<DevServiceLink> links = DevServiceLinks.resolve(service, discoveredContainer.host(), discoveredContainer::mappedPort,
                    DevServiceRegistration.Origin.DISCOVERED);
            DevServiceRegistration registration = createRegistration(service, DevServiceRegistration.Origin.DISCOVERED, containerInfo.getId(), links, sslBundleRegistered);
            DevServiceStartupLogger.ready(registration, containerInfo.getId(), containerInfo.getImageName(),
                    resolveAddress(service, discoveredContainer.host(), discoveredContainer::mappedPort), false, reuse, service.isSslEnabled());
            return registration;
        });

        return beanDefinition;
    }

    // MANAGED

    private GenericBeanDefinition createContainerBeanDefinition(DevServiceDefinition service, ReuseDecision reuse, @Nullable DevServiceSslBundle sslBundle) {
        Supplier<? extends GenericContainer<?>> containerSupplier = service.containerSupplier();

        // Create container bean definition.
        DevServiceContainerBeanDefinition beanDefinition = new DevServiceContainerBeanDefinition();
        BeanAttribute.AOT_EXCLUDED.set(beanDefinition, true);
        beanDefinition.setBeanClass(service.containerType());
        beanDefinition.setDescription(service.description());

        // The annotations Spring Boot reads from the container bean: the service connection, and
        // what makes the application connect over SSL when the dev service is secured with it.
        List<Annotation> annotations = new ArrayList<>();
        if (service.serviceConnection()) {
            Map<String, Object> annotationAttributes = new HashMap<>();
            if (StringUtils.hasText(service.serviceConnectionName())) {
                // Sets the "value" attribute for the @ServiceConnection annotation
                annotationAttributes.put("value", service.serviceConnectionName());
            }
            annotations.add(AnnotationUtils.synthesizeAnnotation(annotationAttributes, ServiceConnection.class, null));
        }
        if (service.isSslEnabled()) {
            annotations.addAll(DevServiceSslSupport.createAnnotations(sslBundle));
        }
        beanDefinition.setAnnotations(MergedAnnotations.from(annotations.toArray(new Annotation[0])));

        // Provide a supplier for creating a Container instance.
        beanDefinition.setInstanceSupplier((InstanceSupplier<GenericContainer<?>>) registeredBean -> {
            GenericContainer<?> container = containerSupplier.get();
            ContainerConfigurer.apply(container, service.properties());
            bindFixedPorts(container, service);
            if (sslBundle != null) {
                DevServiceSslSupport.configure(container, service, sslBundle.bundle());
            }
            container.withReuse(reuse.effective() == ReuseStrategy.TESTCONTAINERS);
            applyCustomizers(container, registeredBean.getBeanFactory());
            applyLabels(container, service, reuse);
            networkSupport.join(container, service.name(), registeredBean.getBeanFactory());
            restartSupport.keepAcrossRestarts(service.name(), container);
            return container;
        });

        // With Spring Boot DevTools, the container is kept across application restarts.
        beanDefinition.setScope(restartSupport.isRestartScopeActive() ? DevServiceRestartSupport.SCOPE_NAME : BeanDefinition.SCOPE_SINGLETON);

        // Hint that this bean has an infrastructure role, meaning it has no relevance to the end-user.
        beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        return beanDefinition;
    }

    private RootBeanDefinition createRegistrationBeanDefinition(DevServiceDefinition service, ReuseDecision reuse, String containerBeanName,
            boolean containerKept, boolean sslBundleRegistered) {
        RootBeanDefinition beanDefinition = newRegistrationBeanDefinition(service);
        beanDefinition.setDependsOn(containerBeanName);

        beanDefinition.setInstanceSupplier((InstanceSupplier<DevServiceRegistration>) registeredBean -> {
            // In an application, the Testcontainers lifecycle has started the container by now, so the links
            // can be resolved against its mapped ports. A container that was never started has no ports to
            // resolve, as when an application context is created without that lifecycle, and so no links.
            GenericContainer<?> container = registeredBean.getBeanFactory().getBean(containerBeanName, GenericContainer.class);
            List<DevServiceLink> links = container.getContainerId() == null ? List.of()
                    : DevServiceLinks.resolve(service, container.getHost(), container::getMappedPort, DevServiceRegistration.Origin.MANAGED);
            DevServiceRegistration registration = createRegistration(service, DevServiceRegistration.Origin.MANAGED, container.getContainerId(), links, sslBundleRegistered);
            String address = container.getContainerId() == null ? null : resolveAddress(service, container.getHost(), container::getMappedPort);
            // The reuse strategy is only reported in dev mode, the only one where it applies, unless
            // the configured one could not be applied, which is worth knowing in any mode.
            DevServiceStartupLogger.ready(registration, container.getContainerId(), imageNameOf(container), address, containerKept,
                    (BootstrapMode.isDev() || reuse.reason() != null) ? reuse : null, service.isSslEnabled());
            return registration;
        });

        return beanDefinition;
    }

    /**
     * The bean definition describing a dev service, before it is told how to resolve the
     * registration. The category of the dev service is recorded on it, so that another dev
     * service of the same category is detected at registration time.
     */
    private static RootBeanDefinition newRegistrationBeanDefinition(DevServiceDefinition service) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(DevServiceRegistration.class);
        BeanAttribute.AOT_EXCLUDED.set(beanDefinition, true);
        beanDefinition.setRole(BeanDefinition.ROLE_SUPPORT);
        if (service.category() != null) {
            BeanAttribute.CATEGORY.set(beanDefinition, service.category());
        }
        return beanDefinition;
    }

    /**
     * Bind the container ports the dev service declares to the fixed host ports configured for
     * them: the main port from the common {@code port} property, and the additional ports from
     * the host ports declared with them. A host port of 0 binds nothing. The bindings the
     * container already declares are kept, since the setter replaces them.
     */
    private static void bindFixedPorts(GenericContainer<?> container, DevServiceDefinition service) {
        List<String> portBindings = new ArrayList<>(container.getPortBindings());
        Integer port = service.port();
        if (port != null && service.properties().getPort() != 0) {
            portBindings.add(service.properties().getPort() + ":" + port);
        }
        for (PortBinding additionalPort : service.additionalPorts()) {
            if (additionalPort.hostPort() != 0) {
                portBindings.add(additionalPort.hostPort() + ":" + additionalPort.containerPort());
            }
        }
        container.setPortBindings(portBindings);
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
    private static void applyLabels(GenericContainer<?> container, DevServiceDefinition service, ReuseDecision reuse) {
        container.withLabel(DevServiceLabels.NAME, service.name());
        if (reuse.effective() == ReuseStrategy.FRAMEWORK) {
            container.withLabel(DevServiceLabels.DISCOVERABLE, "true");
            container.withLabel(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
        }
    }

    /**
     * The address of the main port of the dev service on the host, when the dev service declares
     * one and the container publishes it, for the developer to connect a tool to the service.
     */
    @Nullable
    private static String resolveAddress(DevServiceDefinition service, String host, IntUnaryOperator portMapper) {
        Integer port = service.port();
        if (port == null) {
            return null;
        }
        try {
            return host + ":" + portMapper.applyAsInt(port);
        } catch (Exception ex) {
            logger.debug("Dev Service '{}': port {} could not be resolved", service.name(), port, ex);
            return null;
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
    private static DevServiceRegistration createRegistration(DevServiceDefinition service, DevServiceRegistration.Origin origin,
            @Nullable String containerId, List<DevServiceLink> links, boolean sslBundleRegistered) {
        return DevServiceRegistration.builder()
                .name(service.name())
                .description(service.description())
                .category(service.category())
                .origin(origin)
                .containerInfo(() -> {
                    Assert.notNull(containerId, "containerId cannot be null");
                    return ContainerRuntimeInfo.extractContainerInfoById(containerId);
                })
                .links(links)
                .sslBundleName(sslBundleRegistered ? DevServiceSslSupport.bundleName(service.name()) : null)
                .build();
    }

    /**
     * The beans registered for a dev service, named after it.
     */
    enum BeanName {

        CONTAINER("container"),

        CONNECTION_DETAILS("connectionDetails"),

        REGISTRATION("registration"),

        SSL_BUNDLE("sslBundle");

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
