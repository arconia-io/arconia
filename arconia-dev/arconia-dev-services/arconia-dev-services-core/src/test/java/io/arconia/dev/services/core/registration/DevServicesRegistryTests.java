package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.MapPropertySource;
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
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(container -> container
                                .type(TestPostgresContainer.class)
                                .supplier(TestPostgresContainer::new))))
                .withMessageContaining("service name cannot be null or empty");
    }

    @Test
    void whenContainerSpecIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .properties(TestDevServicesProperties.DEFAULT)))
                .withMessageContaining("service container cannot be null");
    }

    @Test
    void whenContainerTypeIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .properties(TestDevServicesProperties.DEFAULT)
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
                        .properties(TestDevServicesProperties.DEFAULT)
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
                        .properties(TestDevServicesProperties.SHARED)
                        .container(container -> container
                                .type(TestPostgresContainer.class)
                                .supplier(TestPostgresContainer::new))
                        .discovery(discovery -> {})))
                .withMessageContaining("connectionDetailsType cannot be null");
    }

    @Test
    void whenServiceRegisteredTwiceThenBeansRegisteredOnce() {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
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
                .properties(TestDevServicesProperties.DEFAULT)
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
                .properties(TestDevServicesProperties.SHARED)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .discovery(discovery -> discovery
                        .connectionDetails(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).containsEntry(DevServiceLabels.SHARED, "true");
    }

    @Test
    void whenReuseEnabledThenOwnerLabelIsOmitted() {
        enableDevMode();
        DevServicesRegistry registry = registryWithReuseSupport(true);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
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
        enableDevMode();
        DevServicesRegistry registry = registryWithReuseSupport(false);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withReuse(true))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
    }

    @Test
    void whenReuseAndSharedEnabledThenSharedLabelIsTrue() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(null);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.SHARED)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withReuse(true)))
                .discovery(discovery -> discovery
                        .connectionDetails(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        // Reuse and sharing compose: a reused container is still advertised as shared.
        assertThat(container.getLabels()).containsEntry(DevServiceLabels.SHARED, "true");
    }

    @Test
    void whenReuseEnabledThenDiscoveryStillHappens() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.SHARED)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withReuse(true)))
                .discovery(discovery -> discovery
                        .connectionDetails(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()))));

        // An existing shared container is adopted.
        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isFalse();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isTrue();
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
                .properties(TestDevServicesProperties.SHARED)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .discovery(discovery -> discovery
                        // Raw type to bypass the compile-time check and exercise the runtime net.
                        .connectionDetails((Class) OtherConnectionDetails.class,
                                container -> new TestConnectionDetails(container.host()))));

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
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .discovery(discovery -> discovery
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
                .properties(TestDevServicesProperties.SHARED)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .discovery(discovery -> discovery
                        .connectionDetails(TestConnectionDetails.class, container -> {
                            throw new IllegalStateException("boom");
                        })));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenNetworkEnabledWithNetworkBeanThenContainerJoinsNetworkWithAliasLabel() {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        TestNetwork network = new TestNetwork("net-1");
        registerNetworkBean(network);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db", "postgres"))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(network);
        assertThat(container.getLabels()).containsEntry(DevServiceLabels.NETWORK_ALIASES, "db,postgres");
    }

    @Test
    void whenNetworkDisabledThenContainerDoesNotJoinNetwork() {
        registerNetworkBean(new TestNetwork("net-1"));

        // The default registry has no network.enabled property, so the network is off.
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db"))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isNull();
        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.NETWORK_ALIASES);
    }

    @Test
    void whenNetworkEnabledButNoNetworkBeanThenNetworkIsNotAttached() {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db"))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isNull();
        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.NETWORK_ALIASES);
    }

    @Test
    void whenNetworkEnabledWithoutAliasesThenServiceNameIsUsedAsAlias() {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        TestNetwork network = new TestNetwork("net-1");
        registerNetworkBean(network);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(network);
        assertThat(container.getNetworkAliases()).contains("postgres");
        assertThat(container.getLabels()).containsEntry(DevServiceLabels.NETWORK_ALIASES, "postgres");
    }

    @Test
    void whenNetworkEnabledWithoutAliasesThenDoesNotWarn(CapturedOutput output) {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        registerNetworkBean(new TestNetwork("net-1"));

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));

        beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(output).doesNotContain("network alias");
    }

    @Test
    void whenReuseEnabledOnSharedNetworkThenWarns(CapturedOutput output) {
        enableDevMode();
        DevServicesRegistry registry = registryWithNetworkEnabled();
        registerNetworkBean(Network.SHARED);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db").withReuse(true))));

        beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(output).contains("reuse is ineffective").contains("postgres");
    }

    @Test
    void whenContainerHasGeneratedAliasThenItIsExcludedFromLabel() {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        registerNetworkBean(new TestNetwork("net-1"));

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        // Simulate the Testcontainers-generated alias alongside a user-defined one.
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("tc-deadbeef", "db"))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).containsEntry(DevServiceLabels.NETWORK_ALIASES, "db");
    }

    @Test
    void whenMultipleNetworkBeansThenNetworkIsNotAttached(CapturedOutput output) {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        registerNetworkBean(new TestNetwork("net-1"));
        var secondBeanDefinition = new org.springframework.beans.factory.support.RootBeanDefinition();
        secondBeanDefinition.setBeanClass(TestNetwork.class);
        secondBeanDefinition.setInstanceSupplier(() -> new TestNetwork("net-2"));
        beanFactory.registerBeanDefinition("devServicesNetwork2", secondBeanDefinition);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db"))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isNull();
        assertThat(output).contains("no unique Network bean");
    }

    @Test
    void whenNetworkAlreadySetByCustomizerThenItIsHonored() {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        TestNetwork beanNetwork = new TestNetwork("net-bean");
        TestNetwork customizerNetwork = new TestNetwork("net-customizer");
        registerNetworkBean(beanNetwork);
        beanFactory.registerSingleton("networkCustomizer",
                (DevServiceContainerCustomizer<GenericContainer<?>>) container -> container.withNetwork(customizerNetwork));

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(() -> new TestPostgresContainer().withNetworkAliases("db"))));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(customizerNetwork);
    }

    @Test
    void whenContainerProvidesLinksThenRegistrationCapturesThem() {
        registry.registerDevService(service -> service
                .name("linky")
                .properties(TestDevServicesProperties.DEFAULT)
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
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));

        var registration = beanFactory.getBean("devServiceRegistration.postgres", DevServiceRegistration.class);

        assertThat(registration.links()).isEmpty();
    }

    @Test
    void whenSharedServiceRegisteredTwiceThenRuntimeIsNotQueriedAgain() {
        enableDevMode();
        AtomicInteger lookups = new AtomicInteger();
        DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment(), serviceName -> {
            lookups.incrementAndGet();
            return discoveredContainer();
        });

        registerSharedDevService(registry);
        registerSharedDevService(registry);

        // The second registration short-circuits on the existing description bean, so the container
        // runtime is queried only once (no duplicate discovery lookups or logs).
        assertThat(lookups.get()).isEqualTo(1);
        assertThat(beanFactory.containsBeanDefinition("devServiceRegistration.postgres")).isTrue();
    }

    @Test
    void whenLinkProviderThrowsThenRegistrationLinksDegradeToEmpty() {
        registry.registerDevService(service -> service
                .name("throwy")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestThrowingLinkContainer.class)
                        .supplier(TestThrowingLinkContainer::new)));

        var registration = beanFactory.getBean("devServiceRegistration.throwy", DevServiceRegistration.class);

        assertThat(registration.links()).isEmpty();
    }

    @Test
    void whenCustomizerRegisteredInParentContextThenAppliedToMatchingContainer() {
        AtomicBoolean applied = registerTypedCustomizerInParentContext(TestLinkContainer.class);

        registry.registerDevService(service -> service
                .name("linky")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestLinkContainer.class)
                        .supplier(TestLinkContainer::new)));
        beanFactory.getBean("devService.container.linky", GenericContainer.class);

        assertThat(applied).isTrue();
    }

    @Test
    void whenCustomizerRegisteredInParentContextThenNotAppliedToOtherContainer() {
        // The customizer is typed for TestLinkContainer. Its type must be resolved by walking the
        // parent factory chain; without that walk it would resolve to null and wrongly apply here.
        AtomicBoolean applied = registerTypedCustomizerInParentContext(TestLinkContainer.class);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new)));
        beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(applied).isFalse();
    }

    /**
     * Register a {@link DevServiceContainerCustomizer} typed for the given container class as a
     * lambda-backed bean definition in a parent bean factory, and make it the parent of the test
     * bean factory. Returns a flag set when the customizer is actually applied.
     */
    private AtomicBoolean registerTypedCustomizerInParentContext(Class<? extends GenericContainer<?>> containerType) {
        AtomicBoolean applied = new AtomicBoolean(false);
        var parent = new DefaultListableBeanFactory();
        var customizerDefinition = new RootBeanDefinition();
        customizerDefinition.setTargetType(
                ResolvableType.forClassWithGenerics(DevServiceContainerCustomizer.class, containerType));
        customizerDefinition.setInstanceSupplier(() ->
                (DevServiceContainerCustomizer<GenericContainer<?>>) container -> applied.set(true));
        parent.registerBeanDefinition("typedContainerCustomizer", customizerDefinition);
        beanFactory.setParentBeanFactory(parent);
        return applied;
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
     * A registry whose environment enables the global shared network
     * ({@code arconia.dev.services.network.enabled=true}).
     */
    private DevServicesRegistry registryWithNetworkEnabled() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test-network",
                Map.of("arconia.dev.services.network.enabled", "true")));
        return new DevServicesRegistry(beanFactory, environment);
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
        return new DevServicesRegistry(beanFactory, new StandardEnvironment(), serviceName -> result);
    }

    private void registerSharedDevService(DevServicesRegistry registry) {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.SHARED)
                .description("PostgreSQL Dev Service")
                .container(container -> container
                        .type(TestPostgresContainer.class)
                        .supplier(TestPostgresContainer::new))
                .discovery(discovery -> discovery
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

    private static class TestThrowingLinkContainer extends GenericContainer<TestThrowingLinkContainer> implements DevServiceLinkProvider {
        TestThrowingLinkContainer() {
            super("postgres:latest");
        }

        @Override
        public List<DevServiceLink> devServiceLinks() {
            throw new IllegalStateException("link resolution failed");
        }
    }

}
