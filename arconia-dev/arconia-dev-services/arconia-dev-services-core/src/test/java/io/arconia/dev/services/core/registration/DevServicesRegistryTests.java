package io.arconia.dev.services.core.registration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.support.SimpleThreadScope;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DevServicesRegistry}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServicesRegistryTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment());

    private final SimpleThreadScope restartScope = new SimpleThreadScope();

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
                        .container(TestPostgresContainer.class, TestPostgresContainer::new)))
                .withMessageContaining("service name cannot be null or empty");
    }

    @Test
    void whenServiceNameIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestPostgresContainer.class, TestPostgresContainer::new)))
                .withMessageContaining("service name cannot be null or empty");
    }

    @Test
    void whenContainerIsMissingThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .properties(TestDevServicesProperties.DEFAULT)))
                .withMessageContaining("service container type cannot be null");
    }

    @Test
    void whenContainerTypeIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(null, TestPostgresContainer::new)))
                .withMessageContaining("container type cannot be null");
    }

    @Test
    void whenContainerSupplierIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestPostgresContainer.class, null)))
                .withMessageContaining("container supplier cannot be null");
    }

    @Test
    void whenServiceConnectionNameIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> registry.registerDevService(service -> service
                        .name("postgres")
                        .properties(TestDevServicesProperties.DEFAULT)
                        .container(TestPostgresContainer.class, TestPostgresContainer::new)
                        .serviceConnectionName("")))
                .withMessageContaining("serviceConnectionName cannot be null or empty");
    }

    @Test
    void whenServiceConnectionIsEnabledThenContainerHasServiceConnectionAnnotation() {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .serviceConnectionName("postgres"));

        var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition("devService.container.postgres");

        assertThat(beanDefinition.getAnnotations().get(ServiceConnection.class).getString("name")).isEqualTo("postgres");
    }

    @Test
    void whenServiceConnectionIsDisabledThenContainerHasNoServiceConnectionAnnotation() {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .serviceConnection(false));

        var beanDefinition = (DevServiceContainerBeanDefinition) beanFactory.getBeanDefinition("devService.container.postgres");

        assertThat(beanDefinition.getAnnotations().isPresent(ServiceConnection.class)).isFalse();
    }

    @Test
    void whenServiceRegisteredTwiceThenBeansRegisteredOnce() {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        assertThat(beanFactory.getBeanNamesForType(TestPostgresContainer.class)).hasSize(1);
        assertThat(beanFactory.getBeanNamesForType(DevServiceRegistration.class)).hasSize(1);
    }

    @Test
    void whenServiceRegisteredThenContainerIsIdentifiedButNotDiscoverable() {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels())
                .containsEntry(DevServiceLabels.NAME, "postgres")
                .doesNotContainKey(DevServiceLabels.DISCOVERABLE)
                .doesNotContainKey(DevServiceLabels.OWNER);
    }

    @Test
    void whenFrameworkStrategyInDevModeThenContainerIsDiscoverable() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(null);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.FRAMEWORK)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .discovery(TestConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host())));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        // A discoverable container carries the identifier of the application that started it,
        // so that the application never discovers its own containers.
        assertThat(container.getLabels())
                .containsEntry(DevServiceLabels.DISCOVERABLE, "true")
                .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
    }

    @Test
    void whenFrameworkStrategyOutsideDevModeThenContainerIsNotDiscoverable() {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.FRAMEWORK)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .discovery(TestConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host())));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.DISCOVERABLE);
    }

    @Test
    void whenFrameworkStrategyWithoutDiscoveryThenContainerIsNotDiscoverable() {
        enableDevMode();

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.FRAMEWORK)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.DISCOVERABLE);
    }

    @Test
    void whenTestcontainersStrategyThenContainerIsNotDiscoverable() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(null);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.TESTCONTAINERS)
                .container(TestPostgresContainer.class, () -> new TestPostgresContainer().withReuse(true))
                .discovery(TestConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host())));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        // The reuse strategies are mutually exclusive: a container reused via Testcontainers
        // is never discoverable. It carries no per-application label either, which would
        // change the Testcontainers reuse hash at every run.
        assertThat(container.getLabels())
                .doesNotContainKey(DevServiceLabels.DISCOVERABLE)
                .doesNotContainKey(DevServiceLabels.OWNER);
    }

    @Test
    void whenReusedViaTestcontainersThenNoDiscoveryHappens() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.TESTCONTAINERS)
                .container(TestPostgresContainer.class, () -> new TestPostgresContainer().withReuse(true))
                .discovery(TestConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host())));

        // A container started by another application is not discovered: reuse is left to Testcontainers.
        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenContainerDiscoveredThenConnectionDetailsAndRegistrationAreRegistered() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registerSharedDevService(registry);

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isFalse();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isTrue();

        var connectionDetails = beanFactory.getBean("devService.connectionDetails.postgres", TestConnectionDetails.class);
        assertThat(connectionDetails.host()).isEqualTo("localhost");
        assertThat(ContainerImageMetadata.isPresent(beanFactory.getBeanDefinition("devService.connectionDetails.postgres"))).isTrue();
        assertThat(ContainerImageMetadata.getFrom(beanFactory.getBeanDefinition("devService.connectionDetails.postgres")).imageName())
                .isEqualTo("postgres:latest");

        var registration = beanFactory.getBean("devService.registration.postgres", DevServiceRegistration.class);
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
        assertThat(beanFactory.getBean("devService.registration.postgres", DevServiceRegistration.class).origin())
                .isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void whenConnectionDetailsFactoryReturnsWrongTypeThenOwnedContainerIsRegistered() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.FRAMEWORK)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                // Raw type to bypass the compile-time check and exercise the runtime net.
                .discovery((Class) OtherConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host())));

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
    void whenNoneStrategyThenNoDiscoveryHappens() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .discovery(TestConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host())));

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
                .properties(TestDevServicesProperties.FRAMEWORK)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .discovery(TestConnectionDetails.class,
                        container -> {
                            throw new IllegalStateException("boom");
                        }));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenContainerCreatedThenCommonPropertiesAreApplied() {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.WITH_ENVIRONMENT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getEnvMap()).containsEntry("KEY", "value");
        assertThat(container.getNetworkAliases()).contains("db");
    }

    @Test
    void whenTestcontainersStrategyIsEffectiveThenContainerIsReusable() {
        enableDevMode();
        DevServicesRegistry registry = registryWithReuseSupport(true);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.TESTCONTAINERS)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.isShouldBeReused()).isTrue();
    }

    @Test
    void whenNetworkEnabledThenContainerJoinsNetworkWithServiceNameAlias() {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        TestNetwork network = new TestNetwork("net-1");
        registerNetworkBean(network);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, () -> new TestPostgresContainer().withNetworkAliases("db")));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(network);
        // The service name is added next to the user-defined alias.
        assertThat(container.getNetworkAliases()).contains("db", "postgres");
    }

    @Test
    void whenNetworkDisabledThenContainerDoesNotJoinNetwork() {
        registerNetworkBean(new TestNetwork("net-1"));

        // The default registry has no network.enabled property, so the network is off.
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, () -> new TestPostgresContainer().withNetworkAliases("db")));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isNull();
        assertThat(container.getNetworkAliases()).doesNotContain("postgres");
    }

    @Test
    void whenNetworkEnabledButNoNetworkBeanThenContainerCreationFails() {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        assertThatThrownBy(() -> beanFactory.getBean("devService.container.postgres", GenericContainer.class))
                .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
    }

    @Test
    void whenContainerReusedOnDefaultNetworkThenWarns(CapturedOutput output) {
        enableDevMode();
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test-network",
                Map.of("arconia.dev.services.network.enabled", "true")));
        DevServicesRegistry registry = new DevServicesRegistry(beanFactory, environment) {
            @Override
            boolean environmentSupportsTestcontainersReuse() {
                return true;
            }
        };
        registerNetworkBean(Network.SHARED);

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.TESTCONTAINERS)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(output).contains("reuse strategy is ineffective").contains("postgres");
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
                .container(TestPostgresContainer.class, () -> new TestPostgresContainer().withNetworkAliases("db")));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(customizerNetwork);
    }

    @Test
    @SuppressWarnings({"unchecked"})
    void whenCustomizerWithoutBeanDefinitionIsTypedForOtherContainerThenItIsSkipped() {
        // A lambda registered as a singleton has no bean definition to resolve its type from.
        beanFactory.registerSingleton("linkContainerCustomizer",
                (DevServiceContainerCustomizer<TestLinkContainer>) container -> container.withLabel("customized", "true"));

        registerPostgresDevService(registry);

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).doesNotContainKey("customized");
    }

    @Test
    void whenLinksDeclaredThenRegistrationResolvesThemAgainstTheContainer() {
        registry.registerDevService(service -> service
                .name("linky")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestLinkContainer.class, TestLinkContainer::new)
                .link("UI", 8080)
                .link("Docs", 8080, "/docs"));

        var registration = beanFactory.getBean("devService.registration.linky", DevServiceRegistration.class);

        // Links are reported in the order they are declared.
        assertThat(registration.links()).containsExactly(
                new DevServiceLink("UI", "http://localhost:1234"),
                new DevServiceLink("Docs", "http://localhost:1234/docs"));
    }

    @Test
    void whenOwnedLinkPortIsNotExposedThenOnlyThatLinkIsSkipped(CapturedOutput output) {
        registry.registerDevService(service -> service
                .name("linky")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestLinkContainer.class, TestLinkContainer::new)
                .link("UI", 8080)
                .link("Hidden", 9999));

        var registration = beanFactory.getBean("devService.registration.linky", DevServiceRegistration.class);

        // The dev service declares a port its container doesn't expose, which it can fix.
        assertThat(registration.links()).containsExactly(new DevServiceLink("UI", "http://localhost:1234"));
        assertThat(output).contains("Skipping the 'Hidden' link of the 'linky' dev service");
    }

    @Test
    void whenLinkIsInvalidThenRegistrationFails() {
        assertThatIllegalArgumentException().isThrownBy(() -> registry.registerDevService(service -> service
                .name("linky")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestLinkContainer.class, TestLinkContainer::new)
                .link("UI", 8080, "docs")))
                .withMessageContaining("path must be empty or start with '/'");
    }

    @Test
    void whenNoLinksDeclaredThenRegistrationLinksAreEmpty() {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        var registration = beanFactory.getBean("devService.registration.postgres", DevServiceRegistration.class);

        assertThat(registration.links()).isEmpty();
    }

    @Test
    void whenDiscoveredThenLinksAreResolvedAgainstTheDiscoveredContainer() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        registerSharedDevService(registry, service -> service.link("Console", 5432, "/console"));

        var registration = beanFactory.getBean("devService.registration.postgres", DevServiceRegistration.class);

        // The same declaration resolves against the host and published ports of the discovered container.
        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
        assertThat(registration.links()).containsExactly(new DevServiceLink("Console", "http://localhost:54321/console"));
    }

    @Test
    void whenDiscoveredLinkPortIsNotPublishedThenOnlyThatLinkIsSkipped(CapturedOutput output) {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(discoveredContainer());

        // The application that started the container published 5432 but not 9999,
        // which is an expected condition rather than a failure.
        registerSharedDevService(registry, service -> service.link("UI", 5432).link("Hidden", 9999));

        var registration = beanFactory.getBean("devService.registration.postgres", DevServiceRegistration.class);

        assertThat(registration.links()).containsExactly(new DevServiceLink("UI", "http://localhost:54321"));
        assertThat(output).doesNotContain("Skipping the 'Hidden' link");
    }

    @Test
    void whenServiceRegisteredTwiceThenRuntimeIsNotQueriedAgain() {
        enableDevMode();
        AtomicInteger lookups = new AtomicInteger();
        DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment(), serviceName -> {
            lookups.incrementAndGet();
            return List.of(discoveredContainer());
        });

        registerSharedDevService(registry);
        registerSharedDevService(registry);

        // The second registration short-circuits on the existing description bean, so the container
        // runtime is queried only once (no duplicate discovery lookups or logs).
        assertThat(lookups.get()).isEqualTo(1);
        assertThat(beanFactory.containsBeanDefinition("devService.registration.postgres")).isTrue();
    }

    @Test
    void whenCustomizerRegisteredInParentContextThenAppliedToMatchingContainer() {
        AtomicBoolean applied = registerTypedCustomizerInParentContext();

        registry.registerDevService(service -> service
                .name("linky")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestLinkContainer.class, TestLinkContainer::new));
        beanFactory.getBean("devService.container.linky", GenericContainer.class);

        assertThat(applied).isTrue();
    }

    @Test
    void whenCustomizerRegisteredInParentContextThenNotAppliedToOtherContainer() {
        AtomicBoolean applied = registerTypedCustomizerInParentContext();

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));
        beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(applied).isFalse();
    }

    /**
     * Register a {@link DevServiceContainerCustomizer} lambda typed for {@link TestLinkContainer}
     * as a bean definition in a parent bean factory, as a {@code @Bean} method in a parent context
     * would, and make it the parent of the test bean factory. Returns a flag set when the
     * customizer is actually applied.
     */
    private AtomicBoolean registerTypedCustomizerInParentContext() {
        AtomicBoolean applied = new AtomicBoolean(false);
        var parent = new DefaultListableBeanFactory();
        var customizerDefinition = new RootBeanDefinition();
        customizerDefinition.setTargetType(
                ResolvableType.forClassWithGenerics(DevServiceContainerCustomizer.class, TestLinkContainer.class));
        customizerDefinition.setInstanceSupplier(() ->
                (DevServiceContainerCustomizer<TestLinkContainer>) container -> applied.set(true));
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
            boolean environmentSupportsTestcontainersReuse() {
                return supported;
            }
        };
    }

    /**
     * A registry whose shared-container lookup returns the given result
     * instead of querying the container runtime.
     */
    private DevServicesRegistry registryDiscovering(@Nullable DiscoveredContainer result) {
        return new DevServicesRegistry(beanFactory, new StandardEnvironment(), serviceName -> (result != null) ? List.of(result) : List.of());
    }

    private void registerSharedDevService(DevServicesRegistry registry) {
        registerSharedDevService(registry, service -> {});
    }

    private void registerSharedDevService(DevServicesRegistry registry, Consumer<ServiceSpec> customizer) {
        registry.registerDevService(service -> {
            service.name("postgres")
                    .properties(TestDevServicesProperties.FRAMEWORK)
                    .description("PostgreSQL Dev Service")
                    .container(TestPostgresContainer.class, TestPostgresContainer::new)
                    .discovery(TestConnectionDetails.class,
                            container -> new TestConnectionDetails(container.host()));
            customizer.accept(service);
        });
    }

    @Test
    void whenConfigurationChangedAcrossRestartsThenContainerIsKept() {
        enableDevMode();

        RestartingContainer first = restart(Map.of("test.dev.services.postgres.image-name", "postgres:17"),
                TestDevServicesProperties.DEFAULT, List.of());
        RestartingContainer second = restart(Map.of("test.dev.services.postgres.image-name", "postgres:18"),
                TestDevServicesProperties.DEFAULT, List.of());

        assertThat(second).isSameAs(first);
        assertThat(first.stopped).isFalse();
    }

    @Test
    void whenKeptContainerIsNoLongerRunningThenANewOneTakesItsPlace(CapturedOutput output) {
        enableDevMode();

        RestartingContainer first = restart(Map.of(), TestDevServicesProperties.DEFAULT, List.of());
        // The container was removed outside the application.
        first.running = false;
        RestartingContainer second = restart(Map.of(), TestDevServicesProperties.DEFAULT, List.of());

        assertThat(second).isNotSameAs(first);
        assertThat(first.stopped).isFalse();
        assertThat(output).contains("is no longer running");
    }

    @Test
    void whenKeptContainerIsNoLongerRunningThenDiscoveryHappensAgain() {
        enableDevMode();

        RestartingContainer first = restart(Map.of(), TestDevServicesProperties.FRAMEWORK, List.of());
        first.running = false;
        DefaultListableBeanFactory restarted = restartedBeanFactory(Map.of(),
                TestDevServicesProperties.FRAMEWORK, List.of(discoveredContainer()));

        assertThat(restarted.containsBeanDefinition("devService.container.postgres")).isFalse();
        assertThat(restarted.containsBeanDefinition("devService.connectionDetails.postgres")).isTrue();
    }

    @Test
    void whenOwnedContainerKeptAcrossRestartsThenNoDiscoveryHappens() {
        enableDevMode();

        RestartingContainer first = restart(Map.of(), TestDevServicesProperties.FRAMEWORK, List.of());
        // Another application has started a discoverable container in the meantime.
        RestartingContainer second = restart(Map.of(), TestDevServicesProperties.FRAMEWORK, List.of(discoveredContainer()));

        assertThat(second).isSameAs(first);
    }

    @Test
    void whenOldestDiscoveredContainerIsUnusableThenNextOneIsUsed() {
        enableDevMode();
        DiscoveredContainer unusable = new DiscoveredContainer(ContainerInfo.builder()
                .id("def456")
                .imageName("postgres:latest")
                .names(List.of("unusable-postgres"))
                .labels(Map.of(DevServiceLabels.NAME, "postgres", DevServiceLabels.DISCOVERABLE, "true"))
                .status("running")
                .build(), "localhost");
        DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment(),
                serviceName -> List.of(unusable, discoveredContainer()));

        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.FRAMEWORK)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .discovery(TestConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host() + ":" + container.mappedPort(5432))));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isFalse();
        assertThat(beanFactory.getBean("devService.connectionDetails.postgres", TestConnectionDetails.class).host())
                .isEqualTo("localhost:54321");
    }

    @Test
    void whenDevToolsRestartScopeRegisteredInDevModeThenContainerIsRestartScoped() {
        enableDevMode();
        beanFactory.registerScope("restart", restartScope);

        registerPostgresDevService(registry);

        assertThat(beanFactory.getBeanDefinition("devService.container.postgres").getScope()).isEqualTo("restart");
    }

    @Test
    void whenDevToolsRestartIsDisabledThenContainerIsSingleton() {
        enableDevMode();
        beanFactory.registerScope("restart", restartScope);
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test",
                Map.of("spring.devtools.restart.enabled", "false")));

        registerPostgresDevService(new DevServicesRegistry(beanFactory, environment));

        assertThat(beanFactory.getBeanDefinition("devService.container.postgres").getScope()).isEqualTo("singleton");
    }

    @Test
    void whenNotInDevModeThenContainerIsSingleton() {
        beanFactory.registerScope("restart", restartScope);

        registerPostgresDevService(registry);

        assertThat(beanFactory.getBeanDefinition("devService.container.postgres").getScope()).isEqualTo("singleton");
    }

    @Test
    void whenNoRestartScopeRegisteredThenContainerIsSingleton() {
        enableDevMode();

        registerPostgresDevService(registry);

        assertThat(beanFactory.getBeanDefinition("devService.container.postgres").getScope()).isEqualTo("singleton");
    }

    @Test
    void whenReuseStrategyCanBeAppliedThenItIsInEffect() {
        enableDevMode();

        var decision = registry.reuseDecision(serviceSpec(TestDevServicesProperties.FRAMEWORK, true));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.FRAMEWORK);
        assertThat(decision.reason()).isNull();
    }

    @Test
    void whenNotInDevModeThenNoReuseStrategyApplies() {
        var decision = registry.reuseDecision(serviceSpec(TestDevServicesProperties.FRAMEWORK, true));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).isNull();
    }

    @Test
    void whenFrameworkStrategyWithoutDiscoveryThenReuseStrategyIsNotApplied() {
        enableDevMode();

        var decision = registry.reuseDecision(serviceSpec(TestDevServicesProperties.FRAMEWORK, false));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).contains("not supported");
    }

    @Test
    void whenTestcontainersReuseIsNotEnabledThenReuseStrategyIsNotApplied() {
        enableDevMode();

        var decision = registryWithReuseSupport(false).reuseDecision(serviceSpec(TestDevServicesProperties.TESTCONTAINERS, false));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.NONE);
        assertThat(decision.reason()).contains("testcontainers.reuse.enable");
    }

    @Test
    void whenTestcontainersReuseIsEnabledThenReuseStrategyIsInEffect() {
        enableDevMode();

        var decision = registryWithReuseSupport(true).reuseDecision(serviceSpec(TestDevServicesProperties.TESTCONTAINERS, false));

        assertThat(decision.effective()).isEqualTo(ReuseStrategy.TESTCONTAINERS);
        assertThat(decision.reason()).isNull();
    }

    private void registerPostgresDevService(DevServicesRegistry registry) {
        registry.registerDevService(service -> service
                .name("postgres")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));
    }

    private static ServiceSpec serviceSpec(BaseDevServicesProperties properties, boolean discovery) {
        ServiceSpec service = new ServiceSpec()
                .name("postgres")
                .properties(properties)
                .container(TestPostgresContainer.class, TestPostgresContainer::new);
        if (discovery) {
            service.discovery(TestConnectionDetails.class, container -> new TestConnectionDetails(container.host()));
        }
        return service;
    }

    /**
     * Simulate an application start, or a DevTools restart when called again: a new bean factory
     * sharing the same restart scope, in which the dev service is registered with the given
     * configuration, returning the container bean.
     */
    private RestartingContainer restart(Map<String, Object> configuration, BaseDevServicesProperties properties,
            List<DiscoveredContainer> discoveredContainers) {
        return restartedBeanFactory(configuration, properties, discoveredContainers)
                .getBean("devService.container.postgres", RestartingContainer.class);
    }

    private DefaultListableBeanFactory restartedBeanFactory(Map<String, Object> configuration, BaseDevServicesProperties properties,
            List<DiscoveredContainer> discoveredContainers) {
        var restartedBeanFactory = new DefaultListableBeanFactory();
        restartedBeanFactory.registerScope("restart", restartScope);
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", configuration));
        new DevServicesRegistry(restartedBeanFactory, environment, serviceName -> discoveredContainers)
                .registerDevService(service -> service
                        .name("postgres")
                        .properties(properties)
                        .container(RestartingContainer.class, RestartingContainer::new)
                        .discovery(TestConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host())));
        return restartedBeanFactory;
    }

    private static DiscoveredContainer discoveredContainer() {
        return discoveredContainer(Map.of());
    }

    private static DiscoveredContainer discoveredContainer(Map<String, String> additionalLabels) {
        Map<String, String> labels = new HashMap<>(Map.of(DevServiceLabels.NAME, "postgres", DevServiceLabels.DISCOVERABLE, "true"));
        labels.putAll(additionalLabels);
        return new DiscoveredContainer(ContainerInfo.builder()
                .id("abc123")
                .imageName("postgres:latest")
                .names(List.of("shared-postgres"))
                .exposedPorts(List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp")))
                .labels(labels)
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

    /**
     * A container recording whether it was stopped and reporting whether it is running,
     * without requiring a container runtime.
     */
    private static class RestartingContainer extends GenericContainer<RestartingContainer> {

        private boolean stopped;

        private boolean running = true;

        RestartingContainer() {
            super("postgres:latest");
        }

        @Override
        public void stop() {
            stopped = true;
        }

        @Override
        public boolean isRunning() {
            return running;
        }

    }

    private static class TestPostgresContainer extends GenericContainer<TestPostgresContainer> {
        TestPostgresContainer() {
            super("postgres:latest");
        }
    }

    /**
     * A container exposing port 8080 only, with the host and port mapping a started container
     * would report, so that link resolution can be exercised without Docker.
     */
    private static class TestLinkContainer extends GenericContainer<TestLinkContainer> {
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
