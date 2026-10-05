package io.arconia.dev.services.tests;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.testcontainers.containers.GenericContainer;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests of dev services auto-configuration.
 */
public abstract class BaseDevServicesAutoConfigurationIT {

    @TempDir
    protected static Path testMountDir;

    /**
     * The link definitions recorded on the discoverable container by {@link #withDiscoveryLabels}.
     */
    private List<DevServiceLinkDefinition> appliedLinks = List.of();

    /**
     * The application context runner used to execute tests.
     */
    protected abstract ApplicationContextRunner getContextRunner();

    /**
     * The auto-configuration class for the Dev Service to test.
     */
    protected abstract Class<?> getAutoConfigurationClass();

    /**
     * The specific container bean class for the Dev Service to test.
     */
    protected abstract Class<? extends GenericContainer<?>> getContainerClass();

    /**
     * The name of the Dev Service to test.
     */
    protected abstract String getServiceName();

    /**
     * The {@code ConnectionDetails} type expected to be resolvable for this Dev Service,
     * or {@code null} when the Dev Service doesn't provide a service connection
     * (or the corresponding factory is not on the test classpath).
     */
    @Nullable
    protected Class<?> getConnectionDetailsClass() {
        return null;
    }

    /**
     * Create a container labeled as discoverable and owned by {@code ownerId}, as another
     * application would start it, for a Dev Service supporting the {@code framework} reuse
     * strategy. Implementations should return the module's container with the image from its
     * properties, applying the discovery labels via {@link #withDiscoveryLabels}.
     * <p>
     * Returns {@code null} by default, in which case the discovery test below self-skips.
     * <p>
     * The discovery test connects to the oldest discoverable container of the Dev Service
     * running on the machine. A container left running by something else, such as an
     * application started in dev mode with the same Dev Service, makes it fail.
     */
    @Nullable
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        return null;
    }

    /**
     * Apply the discovery labels (name, discoverable, owner) to the given container,
     * matching what another application would set when starting a discoverable container.
     */
    protected GenericContainer<?> withDiscoveryLabels(GenericContainer<?> container, String ownerId) {
        container.withLabel(DevServiceLabels.NAME, getServiceName());
        container.withLabel(DevServiceLabels.DISCOVERABLE, "true");
        container.withLabel(DevServiceLabels.OWNER, ownerId);
        // Reading the declarations constructs a dev service container, which detects and caches the
        // bootstrap mode. Clear it afterwards, or the mode detected here outlives this call and the
        // application under test never sees the dev mode it asks for. For the same reason the links
        // are captured rather than read again later: a dev service whose links depend on the mode
        // would otherwise declare one set here and be expected to report another.
        appliedLinks = discoverableContainerLinkDefinitions();
        BootstrapMode.clear();
        for (DevServiceLinkDefinition link : appliedLinks) {
            DevServiceLabels.linkLabels(link).forEach(container::withLabel);
        }
        return container;
    }

    /**
     * The links another application would record on the discoverable container, so that the discovery
     * test can check they're reported by the application discovering it.
     * <p>
     * Override in a dev service whose container implements {@code DevServiceLinkProvider}, by
     * returning the definitions that container declares rather than by restating them.
     * Empty for a dev service that exposes no links.
     */
    protected List<DevServiceLinkDefinition> discoverableContainerLinkDefinitions() {
        return List.of();
    }

    /**
     * Assert the module-specific connection details resolved for a discovered container.
     * Compare them with what the given container itself reports, so that the test checks they
     * match the ones an application starting the container would get. Default no-op.
     */
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
    }

    @BeforeEach
    void setUp() {
        BootstrapMode.clear();
    }

    @Test
    void autoConfigurationNotActivatedWhenGloballyDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(getContainerClass()));
    }

    @Test
    void autoConfigurationNotActivatedWhenDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.%s.enabled=false".formatted(getServiceName()))
                .run(context -> assertThat(context).doesNotHaveBean(getContainerClass()));
    }

    @Test
    void autoConfigurationNotActivatedInProdMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=prod")
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
                .run(context -> assertThat(context).hasSingleBean(getContainerClass()));
    }

    @Test
    void containerReusedWithTestcontainersStrategy() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.services.%s.reuse-strategy=testcontainers".formatted(getServiceName()))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.isShouldBeReused()).isTrue();
                    // The reuse strategies are mutually exclusive: a container reused via
                    // Testcontainers is never discoverable by other applications.
                    assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.DISCOVERABLE);
                });
    }

    @Test
    void containerDiscoveredWhenStartedByAnotherApplication() {
        GenericContainer<?> peerContainer = createDiscoverableContainer("another-application");
        Assumptions.assumeTrue(peerContainer != null, "dev service does not support discovery");
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

                        // The links come from the labels the peer application recorded, resolved
                        // against the ports that container actually exposes.
                        assertThat(registration.links()).containsExactlyElementsOf(
                                appliedLinks.stream()
                                        .sorted(Comparator.comparing(DevServiceLinkDefinition::id))
                                        .map(link -> link.toLink(discoveredContainer.getHost(),
                                                discoveredContainer.getMappedPort(link.port())))
                                        .toList());

                        assertDiscoveredConnectionDetails(context, discoveredContainer);
                    });
        }
    }

    /**
     * Assert that the given container class is instantiated as a singleton bean in the given application context.
     */
    protected void assertThatHasSingletonScope(AssertableApplicationContext context) {
        String[] beanNames = context.getBeanFactory().getBeanNamesForType(getContainerClass());
        assertThat(beanNames).hasSize(1);
        assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                .isEqualTo("singleton");
    }

    /**
     * Build common configuration properties for a service.
     */
    protected String[] commonConfigurationProperties() {
        String prefix = "arconia.dev.services." + getServiceName();
        return new String[] {
                prefix + ".environment.KEY=value",
                prefix + ".network-aliases=network1",
                prefix + ".resources[0].source-path=test-resource.txt",
                prefix + ".resources[0].container-path=/tmp/test-resource.txt",
                prefix + ".volumes[0].host-path=" + testMountDir.toAbsolutePath(),
                prefix + ".volumes[0].container-path=/arconia"
        };
    }

    /**
     * Assert common configuration properties were applied correctly.
     * Container must be started before calling.
     */
    protected static void assertThatConfigurationIsApplied(GenericContainer<?> container) throws Exception {
        assertThat(container.getEnv()).contains("KEY=value");
        assertThat(container.getNetworkAliases()).contains("network1");
        assertThat(container.getCurrentContainerInfo().getState().getStatus()).isEqualTo("running");

        String mappedResourceContent = container.copyFileFromContainer(
                "/tmp/test-resource.txt",
                inputStream -> new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
        );
        assertThat(mappedResourceContent).isNotEmpty();

        assertThat(container.getBinds()).anyMatch(b -> b.getPath().equals(testMountDir.toAbsolutePath().toString()));
        assertThat(container.getBinds()).anyMatch(b -> b.getVolume().getPath().equals("/arconia"));
    }

    /**
     * Builds a default ApplicationContextRunner for testing auto-configuration with the given auto-configuration class.
     */
    protected static ApplicationContextRunner defaultContextRunner(Class<?> autoConfigurationClass) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(autoConfigurationClass));
    }

}
