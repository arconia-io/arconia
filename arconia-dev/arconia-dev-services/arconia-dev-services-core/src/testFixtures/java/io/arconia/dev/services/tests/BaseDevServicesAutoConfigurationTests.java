package io.arconia.dev.services.tests;

import java.nio.file.Path;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base test class for the auto-configuration of a dev service: its activation, the
 * configuration properties it binds, and the reuse strategies. Only the discovery test needs a
 * container runtime; the others create the container bean without starting it.
 */
public abstract class BaseDevServicesAutoConfigurationTests {

    @TempDir
    protected static Path testMountDir;

    private final ApplicationContextRunner contextRunner;

    private final String serviceName;

    private final Class<? extends GenericContainer<?>> containerClass;

    @Nullable
    private final Class<?> connectionDetailsClass;

    /**
     * Create the tests for the given dev service auto-configuration.
     */
    protected BaseDevServicesAutoConfigurationTests(Class<?> autoConfiguration, String serviceName,
            Class<? extends GenericContainer<?>> containerClass, @Nullable Class<?> connectionDetailsClass) {
        Assert.notNull(autoConfiguration, "autoConfiguration cannot be null");
        Assert.hasText(serviceName, "serviceName cannot be null or empty");
        Assert.notNull(containerClass, "containerClass cannot be null");
        this.contextRunner = new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(autoConfiguration));
        this.serviceName = serviceName;
        this.containerClass = containerClass;
        this.connectionDetailsClass = connectionDetailsClass;
    }

    /**
     * The context runner with the auto-configuration of the dev service. Override it to add
     * what every test of the dev service needs, such as a property or a filtered class loader.
     */
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    /**
     * The context runner with the Spring Boot Testcontainers lifecycle, which starts the dev service
     * container before the beans depending on it are created, as in an application. Use it for tests
     * that need a started container, such as the ones checking the links a dev service reports.
     */
    protected ApplicationContextRunner contextRunnerWithContainerLifecycle() {
        return getContextRunner().withInitializer(new TestcontainersLifecycleApplicationContextInitializer());
    }

    protected String getServiceName() {
        return serviceName;
    }

    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return containerClass;
    }

    @Nullable
    protected Class<?> getConnectionDetailsClass() {
        return connectionDetailsClass;
    }

    /**
     * Create a container labeled as discoverable and owned by {@code ownerId}, as another
     * application would start it, for a dev service supporting the {@code framework} reuse
     * strategy. Implementations should return the module's container with the image from its
     * properties, configured via {@link #asDiscoverableContainer}.
     * <p>
     * Returns {@code null} by default, in which case the discovery test self-skips.
     * <p>
     * The discovery test connects to the oldest discoverable container of the dev service
     * running on the machine. A container left running by something else, such as an
     * application started in dev mode with the same dev service, makes it fail.
     */
    @Nullable
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        return null;
    }

    /**
     * Configure the given container as another application starting it as a discoverable
     * container would: the common dev service properties, and the discovery labels (name,
     * discoverable, owner).
     */
    protected GenericContainer<?> asDiscoverableContainer(GenericContainer<?> container, BaseDevServicesProperties properties, String ownerId) {
        return DevServiceContainers.discoverable(container, properties, getServiceName(), ownerId);
    }

    /**
     * Assert the module-specific connection details resolved for a discovered container.
     * Compare them with what the given container itself reports, so that the test checks they
     * match the ones an application starting the container would get. Default no-op.
     */
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
    }

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.%s.enabled=false".formatted(getServiceName()))
                .run(context -> assertThat(context).doesNotHaveBean(getContainerClass()));
    }

    @Test
    void connectionDetailsAvailableInTestMode() {
        Class<?> connectionDetailsClass = getConnectionDetailsClass();
        Assumptions.assumeTrue(connectionDetailsClass != null,
                "no connection details expected for this dev service");
        getContextRunner()
                .withConfiguration(AutoConfigurations.of(ServiceConnectionAutoConfiguration.class))
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> assertThat(context).hasSingleBean(connectionDetailsClass));
    }

    @Test
    void containerAvailableInTestMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=test")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    // Outside dev mode there is no DevTools restart scope to keep the container in.
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(getContainerClass());
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope()).isEqualTo("singleton");
                });
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.services.%s.reuse-strategy=none".formatted(getServiceName()))
                .run(context -> assertThat(context).hasSingleBean(getContainerClass()));
    }

    @Test
    void commonConfigurationApplied() {
        getContextRunner()
                .withPropertyValues(commonConfigurationProperties())
                .run(context -> {
                    // The common properties are applied by the framework when the container is
                    // created, so they are all visible on the container before it is started.
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getEnvMap()).containsEntry("KEY", "value");
                    assertThat(container.getNetworkAliases()).contains("network1");
                    assertThat(container.getCopyToFileContainerPathMap()).containsValue("/tmp/test-resource.txt");
                    assertThat(container.getBinds())
                            .anyMatch(bind -> bind.getPath().equals(testMountDir.toAbsolutePath().toString())
                                    && bind.getVolume().getPath().equals("/arconia"));
                    // The fixed port is declared by the container when it is configured, right before starting.
                    ReflectionTestUtils.invokeMethod(container, "configure");
                    assertThat(container.getPortBindings()).anyMatch(binding -> binding.startsWith("1234:"));
                });
    }

    @Test
    void containerReusedWithTestcontainersStrategy() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.services.%s.reuse-strategy=testcontainers".formatted(getServiceName()))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    boolean reuseSupported = TestcontainersConfiguration.getInstance().environmentSupportsReuse();
                    assertThat(container.isShouldBeReused()).isEqualTo(reuseSupported);
                    // The reuse strategies are mutually exclusive: a container reused via
                    // Testcontainers is never discoverable by other applications.
                    assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.DISCOVERABLE);
                });
    }

    @Test
    @DisabledIfDockerUnavailable
    void containerDiscoveredWhenStartedByAnotherApplication() {
        GenericContainer<?> peerContainer = createDiscoverableContainer("another-application");
        Assumptions.assumeTrue(peerContainer != null, "dev service does not support discovery");
        // Constructing a dev service container detects and caches the bootstrap mode. Clear it, or
        // the mode detected here outlives this call and the application under test never sees the
        // dev mode it asks for.
        BootstrapMode.clear();
        try (GenericContainer<?> discoveredContainer = peerContainer) {
            discoveredContainer.start();

            getContextRunner()
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .withPropertyValues("arconia.dev.services.%s.reuse-strategy=framework".formatted(getServiceName()))
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(getContainerClass());

                        Class<?> connectionDetailsClass = getConnectionDetailsClass();
                        if (connectionDetailsClass != null) {
                            assertThat(context).hasSingleBean(connectionDetailsClass);
                        }

                        assertThat(context).hasSingleBean(DevServiceRegistration.class);
                        DevServiceRegistration registration = context.getBean(DevServiceRegistration.class);
                        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
                        assertThat(registration.containerInfo().get().id()).isEqualTo(discoveredContainer.getContainerId());

                        // The links the dev service declares are resolved against the host and the
                        // ports the discovered container publishes.
                        List<String> publishedPorts = discoveredContainer.getExposedPorts().stream()
                                .map(port -> String.valueOf(discoveredContainer.getMappedPort(port)))
                                .toList();
                        assertThat(registration.links()).allSatisfy(link -> {
                            assertThat(link.url()).startsWith("http://" + discoveredContainer.getHost() + ":");
                            assertThat(link.url()).matches(url -> publishedPorts.stream().anyMatch(port -> url.contains(":" + port)));
                        });

                        assertDiscoveredConnectionDetails(context, discoveredContainer);
                    });
        }
    }

    /**
     * Build common configuration properties for a service.
     */
    protected String[] commonConfigurationProperties() {
        String prefix = "arconia.dev.services." + getServiceName();
        return new String[] {
                prefix + ".environment.KEY=value",
                prefix + ".network-aliases=network1",
                prefix + ".port=1234",
                prefix + ".resources[0].source-path=test-resource.txt",
                prefix + ".resources[0].container-path=/tmp/test-resource.txt",
                prefix + ".volumes[0].host-path=" + testMountDir.toAbsolutePath(),
                prefix + ".volumes[0].container-path=/arconia"
        };
    }

}
