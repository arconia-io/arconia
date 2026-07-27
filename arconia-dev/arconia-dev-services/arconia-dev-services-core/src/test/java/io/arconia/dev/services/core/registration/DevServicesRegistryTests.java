package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;
import io.arconia.dev.services.core.container.DevServiceLabels;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link DevServicesRegistry}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServicesRegistryTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment());

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
        BootstrapMode.clear();
    }

    @Test
    void whenServiceNameIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name(null)
                        .container(container -> container
                                .type(TestPostgresContainer.class)
                                .supplier(TestPostgresContainer::new))))
                .withMessageContaining("service name cannot be null or empty");
    }

    @Test
    void whenServiceNameIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("")
                        .container(container -> container
                                .type(TestPostgresContainer.class)
                                .supplier(TestPostgresContainer::new))))
                .withMessageContaining("service name cannot be null or empty");
    }

    @Test
    void whenContainerSpecIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")))
                .withMessageContaining("service container cannot be null");
    }

    @Test
    void whenContainerTypeIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .container(container -> container
                                .type(null)
                                .supplier(TestPostgresContainer::new))))
                .withMessageContaining("container type cannot be null");
    }

    @Test
    void whenContainerSupplierIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .container(container -> container
                                .type(TestPostgresContainer.class)
                                .supplier(null))))
                .withMessageContaining("container supplier cannot be null");
    }

    @Test
    void whenSharingConnectionDetailsIsMissingThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .container(container -> container
                                .type(TestPostgresContainer.class)
                                .supplier(TestPostgresContainer::new))
                        .sharing(sharing -> sharing
                                .enabled(true))))
                .withMessageContaining("connectionDetailsType cannot be null");
    }

    @Test
    void whenServiceRegisteredTwiceThenBeansRegisteredOnce() {
        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));
        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));

        assertThat(beanFactory.getBeanNamesForType(TestPostgresContainer.class)).hasSize(1);
        assertThat(beanFactory.getBeanNamesForType(DevServiceRegistration.class)).hasSize(1);
    }

    @Test
    void whenServiceRegisteredThenContainerHasLabels() {
        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels())
                .containsEntry(DevServiceLabels.NAME, "postgres")
                .containsEntry(DevServiceLabels.SHARED, "false")
                .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
    }

    @Test
    void whenSharingEnabledInDevModeThenContainerHasSharedLabel() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(null);

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .sharing(sharing -> sharing
                        .enabled(true)
                        .connectionDetails(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).containsEntry(DevServiceLabels.SHARED, "true");
    }

    @Test
    void whenReuseEnabledThenOwnerLabelIsOmitted() {
        DevServicesRegistry registry = registryWithReuseSupport(true);

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withReuse(true))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels())
                .containsEntry(DevServiceLabels.NAME, "postgres")
                .doesNotContainKey(DevServiceLabels.OWNER);
    }

    @Test
    void whenEnvironmentDoesNotSupportReuseThenOwnerLabelIsKept() {
        DevServicesRegistry registry = registryWithReuseSupport(false);

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withReuse(true))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
    }

    @Test
    void whenReuseEnabledThenSharedLabelIsFalse() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(null);

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withReuse(true)))
                .sharing(sharing -> sharing
                        .enabled(true)
                        .connectionDetails(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).containsEntry(DevServiceLabels.SHARED, "false");
    }

    @Test
    void whenReuseEnabledThenNoDiscoveryHappens() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .sharing(sharing -> sharing
                        .enabled(true)
                        .reuse(true)
                        .connectionDetails(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()))));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenSharedContainerDiscoveredThenConnectionDetailsAndRegistrationAreRegistered() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registerSharedDevService(registry);

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isFalse();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isTrue();
        assertThat(beanFactory.getBeanDefinition("devService.connectionDetails.postgres").getDependsOn())
                .contains(DevServicesRegistry.CONFLICT_VALIDATOR_BEAN_NAME);

        var connectionDetails = beanFactory.getBean("devService.connectionDetails.postgres", TestConnectionDetails.class);
        assertThat(connectionDetails.host()).isEqualTo("localhost");
        assertThat(ContainerImageMetadata.isPresent(beanFactory.getBeanDefinition("devService.connectionDetails.postgres"))).isTrue();
        assertThat(ContainerImageMetadata.getFrom(beanFactory.getBeanDefinition("devService.connectionDetails.postgres")).imageName())
                .isEqualTo("postgres:latest");

        var registration = beanFactory.getBean("devServiceRegistration.postgres", DevServiceRegistration.class);
        assertThat(registration.name()).isEqualTo("postgres");
        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
    }

    @Test
    void whenUserDefinedConnectionDetailsExistsThenDiscoveredConnectionDetailsIsSkipped() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());
        beanFactory.registerSingleton("userConnectionDetails", new TestConnectionDetails("user-defined"));

        registerSharedDevService(registry);

        // The shared container is still adopted, but the user-defined bean takes precedence
        // over the dev-service-provided connection details.
        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isFalse();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
        assertThat(beanFactory.getBean("devServiceRegistration.postgres", DevServiceRegistration.class).origin())
                .isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void whenConnectionDetailsFactoryReturnsWrongTypeThenOwnedContainerIsRegistered() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .sharing(sharing -> sharing
                        // Raw type to bypass the compile-time check and exercise the runtime net.
                        .connectionDetails((Class) OtherConnectionDetails.class,
                                container -> new TestConnectionDetails(container.host()))
                        .enabled(true)));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenNoSharedContainerAvailableThenOwnedContainerIsRegistered() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(null);

        registerSharedDevService(registry);

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenSharingDisabledThenNoDiscoveryHappens() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .sharing(sharing -> sharing
                        .enabled(false)
                        .connectionDetails(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()))));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenNotInDevModeThenNoDiscoveryHappens() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "test");
        BootstrapMode.clear();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registerSharedDevService(registry);

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenConnectionDetailsFactoryFailsThenOwnedContainerIsRegistered() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .sharing(sharing -> sharing
                        .enabled(true)
                        .connectionDetails(TestConnectionDetails.class, container -> {
                            throw new IllegalStateException("boom");
                        })));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenJoinNetworkEnabledWithNetworkBeanThenContainerJoinsNetworkWithAliasLabel() {
        TestNetwork network = new TestNetwork("net-1");
        registerNetworkBean(network);

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db", "postgres")))
                .network(net -> net.enabled(true)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(network);
        assertThat(container.getLabels()).containsEntry(DevServiceLabels.NETWORK_ALIASES, "db,postgres");
    }

    @Test
    void whenJoinNetworkDisabledThenContainerDoesNotJoinNetwork() {
        registerNetworkBean(new TestNetwork("net-1"));

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db")))
                .network(net -> net.enabled(false)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isNull();
        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.NETWORK_ALIASES);
    }

    @Test
    void whenNoNetworkSpecThenContainerDoesNotJoinNetwork() {
        registerNetworkBean(new TestNetwork("net-1"));

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isNull();
    }

    @Test
    void whenJoinNetworkEnabledButNoNetworkBeanThenNetworkIsNotAttached() {
        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db")))
                .network(net -> net.enabled(true)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isNull();
        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.NETWORK_ALIASES);
    }

    @Test
    void whenJoinNetworkEnabledWithoutAliasesThenServiceNameIsUsedAsAlias() {
        TestNetwork network = new TestNetwork("net-1");
        registerNetworkBean(network);

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .network(net -> net.enabled(true)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(network);
        assertThat(container.getNetworkAliases()).contains("postgres");
        assertThat(container.getLabels()).containsEntry(DevServiceLabels.NETWORK_ALIASES, "postgres");
    }

    @Test
    void whenJoinNetworkEnabledWithoutAliasesThenDoesNotWarn(CapturedOutput output) {
        registerNetworkBean(new TestNetwork("net-1"));

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .network(net -> net.enabled(true)));

        beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(output).doesNotContain("network alias");
    }

    @Test
    void whenReuseEnabledOnSharedNetworkThenWarns(CapturedOutput output) {
        registerNetworkBean(Network.SHARED);

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withReuse(true).withNetworkAliases("db")))
                .network(net -> net.enabled(true)));

        beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(output).contains("reuse is ineffective").contains("postgres");
    }

    @Test
    void whenContainerHasGeneratedAliasThenItIsExcludedFromLabel() {
        registerNetworkBean(new TestNetwork("net-1"));

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        // Simulate the Testcontainers-generated alias alongside a user-defined one.
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("tc-deadbeef", "db")))
                .network(net -> net.enabled(true)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).containsEntry(DevServiceLabels.NETWORK_ALIASES, "db");
    }

    @Test
    void whenMultipleNetworkBeansThenNetworkIsNotAttached(CapturedOutput output) {
        registerNetworkBean(new TestNetwork("net-1"));
        var secondBeanDefinition = new org.springframework.beans.factory.support.RootBeanDefinition();
        secondBeanDefinition.setBeanClass(TestNetwork.class);
        secondBeanDefinition.setInstanceSupplier(() -> new TestNetwork("net-2"));
        beanFactory.registerBeanDefinition("devServicesNetwork2", secondBeanDefinition);

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db")))
                .network(net -> net.enabled(true)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isNull();
        assertThat(output).contains("no unique Network bean");
    }

    @Test
    void whenNetworkAlreadySetByCustomizerThenItIsHonored() {
        TestNetwork beanNetwork = new TestNetwork("net-bean");
        TestNetwork customizerNetwork = new TestNetwork("net-customizer");
        registerNetworkBean(beanNetwork);
        beanFactory.registerSingleton("networkCustomizer",
                (DevServiceContainerCustomizer<GenericContainer<?>>) container -> container.withNetwork(customizerNetwork));

        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db")))
                .network(net -> net.enabled(true)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(customizerNetwork);
    }

    @Test
    void whenContainerProvidesLinksThenRegistrationCapturesThem() {
        registry.registerDevService(service -> service
                .name("linky")
                .container(container -> container
                        .type(TestLinkContainer.class)
                        .supplier(TestLinkContainer::new)));

        var registration = beanFactory.getBean("devServiceRegistration.linky", DevServiceRegistration.class);

        assertThat(registration.links()).containsExactly(
                DevServiceLink.builder().id("ui").label("UI").url("http://localhost:1234").build());
    }

    @Test
    void whenContainerProvidesNoLinksThenRegistrationLinksAreEmpty() {
        registry.registerDevService(service -> service
                .name("postgres")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));

        var registration = beanFactory.getBean("devServiceRegistration.postgres", DevServiceRegistration.class);

        assertThat(registration.links()).isEmpty();
    }

    /**
     * Register the given network as a proper bean definition, matching how the auto-configuration
     * exposes it, so it is resolved by {@code getBeanProvider(Network.class).getIfUnique()}.
     */
    private void registerNetworkBean(Network network) {
        var beanDefinition = new org.springframework.beans.factory.support.RootBeanDefinition();
        beanDefinition.setBeanClass(network.getClass());
        beanDefinition.setInstanceSupplier(() -> network);
        beanFactory.registerBeanDefinition("devServicesNetwork", beanDefinition);
    }

    private void enableDevMode() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "dev");
        BootstrapMode.clear();
    }

    /**
     * A registry whose Testcontainers-reuse environment check returns the given value
     * instead of reading the local Testcontainers configuration.
     */
    private DevServicesRegistry registryWithReuseSupport(boolean supported) {
        return new DevServicesRegistry(beanFactory, new StandardEnvironment()) {
            @Override
            boolean environmentSupportsReuse() {
                return supported;
            }
        };
    }

    /**
     * A registry whose shared-container lookup returns the given result
     * instead of querying the container runtime.
     */
    private DevServicesRegistry registryDiscovering(@Nullable DiscoveredContainer result) {
        return new DevServicesRegistry(beanFactory, new StandardEnvironment()) {
            @Override
            @Nullable
            DiscoveredContainer findSharedContainer(String serviceName) {
                return result;
            }
        };
    }

    private void registerSharedDevService(DevServicesRegistry registry) {
        registry.registerDevService(service -> service
                .name("postgres")
                .description("PostgreSQL Dev Service")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .sharing(sharing -> sharing
                        .enabled(true)
                        .connectionDetails(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()))));
    }

    private static DiscoveredContainer discoveredContainer() {
        return new DiscoveredContainer(ContainerInfo.builder()
                .id("abc123")
                .imageName("postgres:latest")
                .names(List.of("shared-postgres"))
                .exposedPorts(List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp")))
                .labels(Map.of(DevServiceLabels.NAME, "postgres", DevServiceLabels.SHARED, "true"))
                .status("running")
                .build(), "localhost");
    }

    private record TestConnectionDetails(String host) implements ConnectionDetails {}

    private record TestNetwork(String id) implements Network {
        @Override
        public String getId() {
            return id;
        }

        @Override
        public void close() {
        }
    }

    private interface OtherConnectionDetails extends ConnectionDetails {}

    private static class TestPostgresContainer extends GenericContainer<TestPostgresContainer> {
        TestPostgresContainer() {
            super("postgres:latest");
        }
    }

    private static class TestLinkContainer extends GenericContainer<TestLinkContainer> implements DevServiceLinkProvider {
        TestLinkContainer() {
            super("postgres:latest");
        }

        @Override
        public List<DevServiceLink> devServiceLinks() {
            return List.of(DevServiceLink.builder().id("ui").label("UI").url("http://localhost:1234").build());
        }
    }

}
