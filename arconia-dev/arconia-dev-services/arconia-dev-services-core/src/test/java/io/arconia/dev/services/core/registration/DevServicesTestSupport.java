package io.arconia.dev.services.core.registration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.core.TestDevServicesProperties;

/**
 * Fixtures shared by the {@link DevServicesRegistry} tests: registries wired for a given
 * scenario, dev service registrations, discovered containers, and the container types
 * used in place of real ones, none of which needs a container runtime.
 */
final class DevServicesTestSupport {

    private DevServicesTestSupport() {}

    // BOOTSTRAP MODE

    static void enableDevMode() {
        setBootstrapMode("dev");
    }

    static void enableTestMode() {
        setBootstrapMode("test");
    }

    static void resetBootstrapMode() {
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
        BootstrapMode.clear();
    }

    private static void setBootstrapMode(String mode) {
        System.setProperty(BootstrapMode.PROPERTY_KEY, mode);
        BootstrapMode.clear();
    }

    // REGISTRIES

    /**
     * A registry whose discovery returns the given container, or nothing, instead of querying
     * the container runtime.
     */
    static DevServicesRegistry registryDiscovering(DefaultListableBeanFactory beanFactory, @Nullable DiscoveredContainer result) {
        return new DevServicesRegistry(beanFactory, new StandardEnvironment(), serviceName -> (result != null) ? List.of(result) : List.of());
    }

    /**
     * A registry whose Testcontainers reuse support check returns the given value instead of
     * reading the local Testcontainers configuration.
     */
    static DevServicesRegistry registryWithReuseSupport(DefaultListableBeanFactory beanFactory, boolean supported) {
        return new DevServicesRegistry(beanFactory, new StandardEnvironment(), serviceName -> List.of(), () -> supported);
    }

    /**
     * A registry whose environment enables the shared network
     * ({@code arconia.dev.services.network.enabled=true}).
     */
    static DevServicesRegistry registryWithNetworkEnabled(DefaultListableBeanFactory beanFactory) {
        return new DevServicesRegistry(beanFactory, networkEnabledEnvironment());
    }

    static StandardEnvironment networkEnabledEnvironment() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test-network",
                Map.of("arconia.dev.services.network.enabled", "true")));
        return environment;
    }

    /**
     * Register the given network as a proper bean definition, matching how the auto-configuration
     * exposes it, so it is resolved by {@code getBeanProvider(Network.class).getIfAvailable(...)}.
     */
    static void registerNetworkBean(DefaultListableBeanFactory beanFactory, Network network) {
        var beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(network.getClass());
        beanDefinition.setInstanceSupplier(() -> network);
        beanFactory.registerBeanDefinition("devServicesNetwork", beanDefinition);
    }

    // REGISTRATIONS

    /**
     * Register a dev service with the default properties and no discovery.
     */
    static void registerPostgresDevService(DevServicesRegistry registry) {
        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));
    }

    /**
     * Register a dev service with the {@code framework} reuse strategy and a discovery.
     */
    static void registerSharedDevService(DevServicesRegistry registry) {
        registerSharedDevService(registry, service -> {});
    }

    static void registerSharedDevService(DevServicesRegistry registry, Consumer<DevServiceSpec> customizer) {
        registry.registerDevService(service -> {
            service.name("postgres")
                    .description("PostgreSQL Dev Service")
                    .properties(TestDevServicesProperties.FRAMEWORK)
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .discovery(TestConnectionDetails.class,
                            container -> new TestConnectionDetails(container.host()));
            customizer.accept(service);
        });
    }

    // DISCOVERED CONTAINERS

    /**
     * A running {@code postgres} container started by another application, publishing port 5432 on 54321.
     */
    static DiscoveredContainer discoveredContainer() {
        return discoveredContainer(Map.of());
    }

    static DiscoveredContainer discoveredContainer(Map<String, String> additionalLabels) {
        Map<String, String> labels = new HashMap<>(Map.of(DevServiceLabels.NAME, "postgres", DevServiceLabels.DISCOVERABLE, "true"));
        labels.putAll(additionalLabels);
        return new DiscoveredContainer(ContainerInfo.builder()
                .id("abc123")
                .imageName("postgres:latest")
                .names(List.of("shared-postgres"))
                .exposedPorts(List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp")))
                .labels(labels)
                .state("running")
                .status("running")
                .build(), "localhost");
    }

    // TYPES

    record TestConnectionDetails(String host) implements ConnectionDetails {}

    @FunctionalInterface
    interface OtherConnectionDetails extends ConnectionDetails {
        void connect();
    }

    record TestNetwork(String id) implements Network {
        @Override
        public String getId() {
            return id;
        }

        @Override
        public void close() {
        }
    }

    static class TestPostgresContainer extends GenericContainer<TestPostgresContainer> {
        TestPostgresContainer() {
            super("postgres:latest");
        }
    }

    /**
     * A container exposing port 8080 only, with the host and port mapping a started container
     * would report, so that link resolution can be exercised without Docker.
     */
    static class TestLinkContainer extends GenericContainer<TestLinkContainer> {
        TestLinkContainer() {
            super("postgres:latest");
        }

        @Override
        public String getHost() {
            return "localhost";
        }

        @Override
        public Integer getMappedPort(int originalPort) {
            if (originalPort != 8080) {
                throw new IllegalArgumentException("Requested port (" + originalPort + ") is not mapped");
            }
            return 1234;
        }
    }

}
