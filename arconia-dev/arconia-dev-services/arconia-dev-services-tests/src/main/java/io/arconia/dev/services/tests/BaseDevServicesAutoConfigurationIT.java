package io.arconia.dev.services.tests;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.support.SimpleThreadScope;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestcontainersConfiguration;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.container.DevServiceLabels;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for integration tests of dev services auto-configuration.
 */
public abstract class BaseDevServicesAutoConfigurationIT {

    @TempDir
    protected static Path testMountDir;

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
     * Whether this Dev Service supports sharing and discovery. When {@code false}, the
     * sharing/discovery tests below self-skip. Override to return {@code true} and implement
     * {@link #createSharedContainer(String)}.
     */
    protected boolean supportsSharing() {
        return false;
    }

    /**
     * Whether to run the discovery-selection probes that provision multiple shared containers or
     * manipulate container state ({@code oldest-wins}, {@code paused-skipped}, {@code own-skipped}).
     * These exercise generic registry selection logic that is identical across dev services, so a
     * composed-container dev service (one that provisions extra containers internally, such as
     * OpenLit with its ClickHouse backend) can override this to {@code false} to avoid spinning up
     * multiple full stacks. The single-container discovery test still runs. Defaults to
     * {@link #supportsSharing()}.
     */
    protected boolean supportsSharedContainerDiscoveryProbing() {
        return supportsSharing();
    }

    /**
     * Create a library container labeled as a shared Dev Service owned by {@code ownerId},
     * as another application would start it. Only invoked when {@link #supportsSharing()} is
     * {@code true}. Implementations should return the module's container with the image from its
     * properties. The shared-service labels are applied via {@link #withSharedLabels}.
     */
    protected GenericContainer<?> createSharedContainer(String ownerId) {
        throw new UnsupportedOperationException(
                "createSharedContainer must be overridden when supportsSharing() returns true");
    }

    /**
     * Apply the standard shared Dev Service labels (name, shared, owner) to the given container,
     * matching what a peer application would set when starting a shared container.
     */
    protected GenericContainer<?> withSharedLabels(GenericContainer<?> container, String ownerId) {
        container.withLabel(DevServiceLabels.NAME, getServiceName());
        container.withLabel(DevServiceLabels.SHARED, "true");
        container.withLabel(DevServiceLabels.OWNER, ownerId);
        return container;
    }

    /**
     * Assert the module-specific connection details resolved for a discovered shared container
     * (for example, that ports and credentials match). Default no-op; override to add checks.
     */
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> sharedContainer) {
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
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.isShouldBeReused()).isFalse();
                });
    }

    @Test
    void containerWithRestartScope() {
        getContextRunner()
                .withClassLoader(this.getClass().getClassLoader())
                .withInitializer(context ->
                        context.getBeanFactory().registerScope("restart", new SimpleThreadScope()))
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    String[] beanNames = context.getBeanFactory().getBeanNamesForType(getContainerClass());
                    assertThat(beanNames).hasSize(1);
                    assertThat(context.getBeanFactory().getBeanDefinition(beanNames[0]).getScope())
                            .isEqualTo("restart");
                });
    }

    @Test
    void containerReusedWhenReuseEnabled() {
        Assumptions.assumeTrue(supportsSharing(), "dev service does not support sharing/reuse");
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.services.%s.reuse=true".formatted(getServiceName()))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.isShouldBeReused()).isTrue();
                    // Sharing and reuse compose: a reused container is still advertised as shared.
                    assertThat(container.getLabels()).containsEntry(DevServiceLabels.SHARED, "true");
                    // The owner label is omitted for reusable containers (user labels contribute
                    // to the Testcontainers reuse hash), but only when the environment actually
                    // supports reuse; otherwise it's kept to protect against self-discovery.
                    if (TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
                        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.OWNER);
                    } else {
                        assertThat(container.getLabels()).containsKey(DevServiceLabels.OWNER);
                    }
                });
    }

    @Test
    void containerNotSharedWhenSharingDisabled() {
        Assumptions.assumeTrue(supportsSharing(), "dev service does not support sharing");
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .withPropertyValues("arconia.dev.services.%s.shared=false".formatted(getServiceName()))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getLabels()).containsEntry(DevServiceLabels.SHARED, "false");
                });
    }

    @Test
    void sharedContainerDiscoveredWhenStartedByAnotherApplication() {
        Assumptions.assumeTrue(supportsSharing(), "dev service does not support sharing");
        try (GenericContainer<?> sharedContainer = createSharedContainer("another-application")) {
            sharedContainer.start();

            getContextRunner()
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(getContainerClass());

                        Class<?> connectionDetailsClass = getConnectionDetailsClass();
                        if (connectionDetailsClass != null) {
                            assertThat(context).hasSingleBean(connectionDetailsClass);
                        }

                        assertThat(context).hasSingleBean(DevServiceRegistration.class);
                        DevServiceRegistration registration = context.getBean(DevServiceRegistration.class);
                        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
                        assertThat(registration.containerInfo().get().id()).isEqualTo(sharedContainer.getContainerId());

                        assertDiscoveredConnectionDetails(context, sharedContainer);
                    });
        }
    }

    @Test
    void oldestSharedContainerDiscoveredWhenMultipleAvailable() throws Exception {
        Assumptions.assumeTrue(supportsSharedContainerDiscoveryProbing(), "dev service does not support discovery-selection probing");
        try (GenericContainer<?> olderContainer = createSharedContainer("another-application");
             GenericContainer<?> newerContainer = createSharedContainer("yet-another-application")) {
            olderContainer.start();
            // Container creation timestamps have second granularity.
            Thread.sleep(1100);
            newerContainer.start();

            getContextRunner()
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        DevServiceRegistration registration = context.getBean(DevServiceRegistration.class);
                        assertThat(registration.containerInfo().get().id()).isEqualTo(olderContainer.getContainerId());
                    });
        }
    }

    @Test
    void pausedSharedContainerNotDiscovered() {
        Assumptions.assumeTrue(supportsSharedContainerDiscoveryProbing(), "dev service does not support discovery-selection probing");
        try (GenericContainer<?> pausedContainer = createSharedContainer("another-application")) {
            pausedContainer.start();
            DockerClientFactory.lazyClient().pauseContainerCmd(pausedContainer.getContainerId()).exec();

            try {
                getContextRunner()
                        .withSystemProperties("arconia.bootstrap.mode=dev")
                        .run(context -> {
                            assertThat(context).hasSingleBean(getContainerClass());
                            assertThat(context.getBean(DevServiceRegistration.class).origin())
                                    .isEqualTo(DevServiceRegistration.Origin.OWNED);
                        });
            } finally {
                DockerClientFactory.lazyClient().unpauseContainerCmd(pausedContainer.getContainerId()).exec();
            }
        }
    }

    @Test
    void ownSharedContainerNotDiscovered() {
        Assumptions.assumeTrue(supportsSharedContainerDiscoveryProbing(), "dev service does not support discovery-selection probing");
        try (GenericContainer<?> ownContainer = createSharedContainer(DevServiceLabels.ownerId())) {
            ownContainer.start();

            getContextRunner()
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        assertThat(context).hasSingleBean(getContainerClass());
                        assertThat(context.getBean(DevServiceRegistration.class).origin())
                                .isEqualTo(DevServiceRegistration.Origin.OWNED);
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
                .withClassLoader(new FilteredClassLoader(RestartScope.class))
                .withConfiguration(AutoConfigurations.of(autoConfigurationClass));
    }

}
