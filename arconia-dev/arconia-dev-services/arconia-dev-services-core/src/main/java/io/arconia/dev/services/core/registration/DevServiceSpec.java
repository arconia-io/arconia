package io.arconia.dev.services.core.registration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import io.arconia.core.annotations.Incubating;
import io.arconia.dev.services.api.config.DevServicesProperties;
import io.arconia.dev.services.api.config.SslDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServiceDefinition.Discovery;
import io.arconia.dev.services.core.registration.DevServiceDefinition.LinkDefinition;
import io.arconia.dev.services.core.registration.DevServiceDefinition.PortBinding;
import io.arconia.dev.services.core.registration.DevServiceDefinition.SslSupport;
import io.arconia.dev.services.core.ssl.DevServiceSslConfigurer;
import io.arconia.dev.services.core.ssl.SslKeyStoreRequirement;

/**
 * Specification of a single dev service.
 */
@Incubating
public final class DevServiceSpec {

    /**
     * The names a dev service can be given. The name becomes part of bean names, container
     * labels, and the default network alias of the container, so it is kept to the character
     * set valid in all of them.
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*+");

    @Nullable
    private String name;

    @Nullable
    private String title;

    @Nullable
    private String documentation;

    @Nullable
    private Class<?> registrarType;

    @Nullable
    private String icon;

    @Nullable
    private String description;

    @Nullable
    private String category;

    @Nullable
    private DevServicesProperties properties;

    @Nullable
    private Class<? extends GenericContainer<?>> containerType;

    @Nullable
    private Supplier<? extends GenericContainer<?>> containerSupplier;

    private boolean serviceConnection = true;

    @Nullable
    private String serviceConnectionName;

    @Nullable
    private Class<? extends ConnectionDetails> connectionDetailsType;

    @Nullable
    private Function<DiscoveredContainer, ? extends ConnectionDetails> connectionDetailsFactory;

    @Nullable
    private Integer port;

    private final List<PortBinding> additionalPorts = new ArrayList<>();

    private final List<LinkDefinition> links = new ArrayList<>();

    @Nullable
    private SslSupport sslSupport;

    DevServiceSpec() {}

    /**
     * The logical name of the dev service.
     */
    public DevServiceSpec name(String name) {
        this.name = name;
        return this;
    }

    /**
     * The title of the dev service, which is the name of the product it runs, such as
     * {@code PostgreSQL}. It is how the dev service is presented to the developer.
     */
    public DevServiceSpec title(String title) {
        this.title = title;
        return this;
    }

    /**
     * What the dev service provides, in a sentence. It is optional, and worth setting when
     * there is more to say than the title.
     */
    public DevServiceSpec description(@Nullable String description) {
        this.description = description;
        return this;
    }

    /**
     * The URL of the documentation of the dev service, which developer tooling links to. The
     * dev services of Arconia link to their page in the documentation of Arconia by default.
     */
    public DevServiceSpec documentation(@Nullable String documentation) {
        this.documentation = documentation;
        return this;
    }

    /**
     * The class that registers the dev service, which tells whether it is one of the dev
     * services of Arconia.
     */
    DevServiceSpec registeredBy(@Nullable Class<?> registrarType) {
        this.registrarType = registrarType;
        return this;
    }

    /**
     * The documentation set for the dev service or, for a dev service of Arconia without one,
     * its page in the documentation of Arconia.
     */
    private @Nullable String resolveDocumentation(String name) {
        return (documentation != null) ? documentation : DevServiceConventions.documentation(registrarType, name);
    }

    /**
     * What the names of the configuration properties of the dev service start with: the prefix
     * their class is annotated with, which is how the properties of a dev service are declared.
     * Developer tooling uses it to show the properties of the dev service.
     */
    private static @Nullable String propertiesPrefix(DevServicesProperties properties) {
        ConfigurationProperties annotation = AnnotatedElementUtils.findMergedAnnotation(ClassUtils.getUserClass(properties),
                ConfigurationProperties.class);
        return (annotation != null && StringUtils.hasText(annotation.prefix())) ? annotation.prefix() : null;
    }

    /**
     * The name of the icon that developer tooling, such as the Arconia Dev UI, shows for the
     * dev service. It is the name of an icon of the <a href="https://lucide.dev/icons">Lucide</a>
     * icon set, such as {@code database}. A dev service without an icon is shown without one.
     */
    public DevServiceSpec icon(@Nullable String icon) {
        this.icon = icon;
        return this;
    }

    /**
     * The category of mutually exclusive dev services this dev service belongs to, if any.
     * Only one dev service per category can be active in an application: registering a second
     * one fails at startup. Use the {@link DevServiceCategories} constants to share a category
     * with the built-in dev services.
     */
    public DevServiceSpec category(String category) {
        this.category = category;
        return this;
    }

    /**
     * The configuration properties of the dev service.
     */
    public DevServiceSpec properties(DevServicesProperties properties) {
        this.properties = properties;
        return this;
    }

    /**
     * The container to register for the dev service: its type and a supplier
     * function providing the instance.
     */
    public <C extends GenericContainer<?>> DevServiceSpec container(Class<C> type, Supplier<? extends C> supplier) {
        this.containerType = type;
        this.containerSupplier = supplier;
        return this;
    }

    /**
     * Whether the {@link ServiceConnection} annotation is added to the registered container
     * bean, for Spring Boot to provide the connection details for the container. Enabled by default.
     * <p>
     * Disable it when no {@code ContainerConnectionDetailsFactory} is available for the
     * container, since Spring Boot fails to start an application having a service
     * connection that provides no connection details. In that case, the application is
     * connected to the dev service via properties instead (see {@link DevServicesRegistrar#addDynamicProperty(String, Supplier)}).
     * A dev service with the service connection disabled can still declare its
     * {@link #discovery(Class, Function) discovery}: a discovered container is then exposed to the
     * application through the declared {@code ConnectionDetails}, while a container the
     * application starts is exposed through the properties.
     */
    public DevServiceSpec serviceConnection(boolean serviceConnection) {
        this.serviceConnection = serviceConnection;
        return this;
    }

    /**
     * The name of the {@link ServiceConnection} annotation added to the registered container bean.
     * <p>
     * By default, the annotation is added with no explicit name, and Spring Boot determines
     * the connection details to provide from the container type. Set a name when they are
     * determined from the name of the service instead, which is the case when no
     * technology-specific container type is available or supported by Spring Boot.
     */
    public DevServiceSpec serviceConnectionName(String serviceConnectionName) {
        this.serviceConnectionName = serviceConnectionName;
        return this;
    }

    /**
     * How to connect to the dev service running in a container started by another
     * application: the type of {@link ConnectionDetails} provided for the dev service and a
     * factory function providing the instance for a container discovered as running.
     * <p>
     * With the {@code framework} reuse strategy, the dev service container is discoverable by
     * other applications. The application connects to a running container (used as it
     * runs, with the configuration of the application that started it) if one is discovered,
     * instead of starting a new one. A dev service that declares no discovery never participates
     * in it, even when the {@code framework} reuse strategy is configured.
     * <p>
     * The declared type is used to look up existing user-defined {@code ConnectionDetails}
     * beans: when one is present, it takes precedence over the one provided by the dev service.
     */
    public <D extends ConnectionDetails> DevServiceSpec discovery(Class<D> connectionDetailsType, Function<DiscoveredContainer, ? extends D> connectionDetails) {
        this.connectionDetailsType = connectionDetailsType;
        this.connectionDetailsFactory = connectionDetails;
        return this;
    }

    /**
     * The container port of the main service the dev service provides, which the common
     * {@code port} property binds to a fixed port on the host when set. The framework applies
     * the binding when it creates the container. A dev service declaring no port can't be
     * given a fixed one: setting the {@code port} property for it fails at startup.
     */
    public DevServiceSpec port(int containerPort) {
        this.port = containerPort;
        return this;
    }

    /**
     * Another container port the dev service binds to the given fixed port on the host, such as
     * the port of a management console configured through a property of the dev service. A host
     * port of 0, the default of such a property, binds nothing: the container port is published
     * on a random available port. The framework applies the binding when it creates the container.
     */
    public DevServiceSpec additionalPort(int containerPort, int hostPort) {
        additionalPorts.add(new PortBinding(containerPort, hostPort));
        return this;
    }

    /**
     * A link the dev service exposes, such as a management console or an administration API,
     * at the given container port. The link is resolved against the port mapping of the
     * container the dev service runs in, whether started by this application or discovered.
     */
    public DevServiceSpec link(String label, int port) {
        return link(label, port, "");
    }

    /**
     * A link the dev service exposes at the given container port and path.
     */
    public DevServiceSpec link(String label, int port, String path) {
        links.add(new LinkDefinition(label, port, path));
        return this;
    }

    /**
     * How to configure the container to use SSL when the {@code ssl.enabled} property of the
     * dev service is true. Declaring it is what opts the dev service into SSL: its properties
     * must implement {@link SslDevServicesProperties}, and setting the property for a dev service
     * declaring no SSL support fails at startup. The configurer receives the SSL bundle built
     * from the {@code ssl} properties, which must configure a key store. SSL support requires
     * the service connection, through which Spring Boot connects the application over SSL.
     */
    public <C extends Container<?>> DevServiceSpec ssl(DevServiceSslConfigurer<C> configurer) {
        return ssl(configurer, SslKeyStoreRequirement.REQUIRED);
    }

    /**
     * How to configure the container to use SSL when the {@code ssl.enabled} property of the
     * dev service is true, stating whether a key store is required. A dev service whose
     * container generates its own certificate when none is configured declares
     * {@link SslKeyStoreRequirement#OPTIONAL}: the configurer is then only called when a key store is
     * configured, and Spring Boot extracts the generated certificate from the container otherwise.
     */
    public <C extends Container<?>> DevServiceSpec ssl(DevServiceSslConfigurer<C> configurer, SslKeyStoreRequirement keyStoreRequirement) {
        this.sslSupport = new SslSupport(configurer, keyStoreRequirement);
        return this;
    }

    /**
     * Check that the specification is complete and consistent, and build the definition of the
     * dev service from it. Every failure names the dev service, once its name is known.
     */
    DevServiceDefinition build() {
        Assert.hasText(name, "Dev Service name cannot be null or empty");
        Assert.isTrue(NAME_PATTERN.matcher(name).matches(),
                () -> "Dev Service name must contain only lowercase letters, digits, and single dashes between them: " + name);
        String prefix = "Dev Service '%s': ".formatted(name);

        Assert.hasText(title, () -> prefix + "title cannot be null or empty");
        Assert.isTrue(description == null || StringUtils.hasText(description), () -> prefix + "description cannot be empty");
        Assert.isTrue(icon == null || NAME_PATTERN.matcher(icon).matches(),
                () -> prefix + "icon must contain only lowercase letters, digits, and single dashes between them: " + icon);
        Assert.isTrue(documentation == null || documentation.startsWith("http://") || documentation.startsWith("https://"),
                () -> prefix + "documentation must be an HTTP or HTTPS URL: " + documentation);
        Assert.isTrue(category == null || StringUtils.hasText(category), () -> prefix + "category cannot be empty");
        Assert.notNull(properties, () -> prefix + "properties cannot be null");
        Assert.notNull(containerType, () -> prefix + "container type cannot be null");
        Assert.notNull(containerSupplier, () -> prefix + "container supplier cannot be null");
        Assert.state(serviceConnection || serviceConnectionName == null,
                () -> prefix + "a service connection name has no effect when the service connection is disabled");
        Assert.isTrue(serviceConnectionName == null || StringUtils.hasText(serviceConnectionName),
                () -> prefix + "service connection name cannot be empty");
        Assert.state(sslSupport == null || serviceConnection,
                () -> prefix + "SSL support requires the service connection");

        validateProperties(prefix, properties);
        validatePort(prefix, properties);
        links.forEach(link -> validateLink(prefix, link));
        DevServiceSslSupport.validate(prefix, properties, sslSupport);

        return DevServiceDefinition.builder()
                .name(name)
                .title(title)
                .description(description)
                .documentation(resolveDocumentation(name))
                .propertiesPrefix(propertiesPrefix(properties))
                .icon(icon)
                .category(category)
                .properties(properties)
                .container(containerType, containerSupplier)
                .serviceConnection(serviceConnection)
                .serviceConnectionName(serviceConnectionName)
                .discovery(buildDiscovery(prefix))
                .port(port)
                .additionalPorts(additionalPorts)
                .links(links)
                .sslSupport(sslSupport)
                .build();
    }

    /**
     * Check the declared container ports and the fixed host ports they are bound to, and that
     * a fixed port configured through the {@code port} property has a container port to bind
     * to, so that the property is never silently ignored.
     */
    private void validatePort(String prefix, DevServicesProperties properties) {
        Set<Integer> containerPorts = new HashSet<>();
        if (port != null) {
            requirePort(prefix, port, "container port");
            containerPorts.add(port);
        }
        int fixedPort = properties.getPort();
        Assert.isTrue(fixedPort >= 0 && fixedPort <= 65535,
                () -> prefix + "the 'port' property must be between 0 and 65535, but was: " + fixedPort);
        Assert.state(port != null || fixedPort == 0,
                () -> prefix + "the 'port' property is set to " + fixedPort + ", but the dev service declares no container port to bind it to");
        for (PortBinding additionalPort : additionalPorts) {
            requirePort(prefix, additionalPort.containerPort(), "additional container port");
            Assert.isTrue(additionalPort.hostPort() >= 0 && additionalPort.hostPort() <= 65535,
                    () -> prefix + "the host port for container port " + additionalPort.containerPort() + " must be between 0 and 65535, but was: " + additionalPort.hostPort());
            Assert.isTrue(containerPorts.add(additionalPort.containerPort()),
                    () -> prefix + "container port " + additionalPort.containerPort() + " is declared more than once");
        }
    }

    /**
     * The discovery, when declared. Both the type and the factory are needed, so declaring
     * either one as null is an error rather than a way to opt out.
     */
    @Nullable
    private Discovery buildDiscovery(String prefix) {
        if (connectionDetailsType == null && connectionDetailsFactory == null) {
            return null;
        }
        Assert.notNull(connectionDetailsType, () -> prefix + "discovery connection details type cannot be null");
        Assert.notNull(connectionDetailsFactory, () -> prefix + "discovery connection details factory cannot be null");
        return new Discovery(connectionDetailsType, connectionDetailsFactory);
    }

    /**
     * Check the values the common properties contract requires, so that an implementation
     * returning null, as a hand-written properties class may, is reported by property name
     * rather than failing deep inside the framework or Testcontainers.
     */
    private static void validateProperties(String prefix, DevServicesProperties properties) {
        Assert.hasText(properties.getImageName(), () -> prefix + "the 'image-name' property cannot be null or empty");
        Assert.notNull(properties.getEnvironment(), () -> prefix + "the 'environment' property cannot be null");
        Assert.notNull(properties.getNetworkAliases(), () -> prefix + "the 'network-aliases' property cannot be null");
        Assert.notNull(properties.getResources(), () -> prefix + "the 'resources' property cannot be null");
        Assert.notNull(properties.getReuseStrategy(), () -> prefix + "the 'reuse-strategy' property cannot be null");
        Assert.notNull(properties.getVolumes(), () -> prefix + "the 'volumes' property cannot be null");
        Duration startupTimeout = properties.getStartupTimeout();
        Assert.notNull(startupTimeout, () -> prefix + "the 'startup-timeout' property cannot be null");
        Assert.isTrue(startupTimeout.isPositive(), () -> prefix + "the 'startup-timeout' property must be positive, but was: " + startupTimeout);
    }

    private static void validateLink(String prefix, LinkDefinition link) {
        Assert.hasText(link.label(), () -> prefix + "link label cannot be null or empty");
        requirePort(prefix, link.port(), "link port");
        Assert.isTrue(link.path().isEmpty() || link.path().startsWith("/"),
                () -> prefix + "link path must be empty or start with '/': " + link.path());
    }

    private static void requirePort(String prefix, int port, String what) {
        Assert.isTrue(port > 0 && port <= 65535, () -> prefix + what + " must be between 1 and 65535, but was: " + port);
    }

}
