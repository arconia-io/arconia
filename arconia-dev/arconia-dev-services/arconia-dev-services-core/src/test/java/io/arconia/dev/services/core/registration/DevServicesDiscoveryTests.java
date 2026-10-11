package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.OtherConnectionDetails;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestConnectionDetails;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.discoveredContainer;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableDevMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableTestMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerSharedDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registryDiscovering;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesRegistry}: the discovery of containers started by other
 * applications, and the labels making the application's own containers discoverable.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServicesDiscoveryTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment());

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void whenFrameworkStrategyInDevModeThenContainerIsDiscoverable() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(beanFactory, null);

        registerSharedDevService(registry);

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        // A discoverable container carries the identifier of the application that started it,
        // so that the application never discovers its own containers.
        assertThat(container.getLabels())
                .containsEntry(DevServiceLabels.DISCOVERABLE, "true")
                .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());
    }

    @Test
    void whenFrameworkStrategyOutsideDevModeThenContainerIsNotDiscoverable() {
        registerSharedDevService(registry);

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.DISCOVERABLE);
    }

    @Test
    void whenFrameworkStrategyWithoutDiscoveryThenContainerIsNotDiscoverable() {
        enableDevMode();

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.FRAMEWORK)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getLabels()).doesNotContainKey(DevServiceLabels.DISCOVERABLE);
    }

    @Test
    void whenTestcontainersStrategyThenContainerIsNotDiscoverable() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(beanFactory, null);

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
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
        DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
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
        DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

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
    void whenApplicationDefinesConnectionDetailsThenDiscoverableDevServiceIsNotRegistered(CapturedOutput output) {
        enableDevMode();
        AtomicInteger lookups = new AtomicInteger();
        DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment(), serviceName -> {
            lookups.incrementAndGet();
            return List.of(discoveredContainer());
        });
        beanFactory.registerSingleton("userConnectionDetails", new TestConnectionDetails("user-defined"));

        registerSharedDevService(registry);

        // The application connects to the service of its choice: the dev service is neither
        // started nor discovered, so the container runtime is never queried.
        assertThat(beanFactory.getBeanDefinitionNames()).noneMatch(name -> name.startsWith("devService."));
        assertThat(lookups.get()).isZero();
        assertThat(output).contains("Dev Service 'postgres' is not started: the application defines its own TestConnectionDetails");
    }

    @Test
    void whenApplicationDefinesConnectionDetailsThenManagedDevServiceIsNotRegistered() {
        beanFactory.registerSingleton("userConnectionDetails", new TestConnectionDetails("user-defined"));

        // Outside dev mode no discovery happens, so the container would have been started by the application.
        registerSharedDevService(registry);

        assertThat(beanFactory.getBeanDefinitionNames()).noneMatch(name -> name.startsWith("devService."));
    }

    @Test
    void whenApplicationDefinesOtherConnectionDetailsThenDevServiceIsRegistered() {
        beanFactory.registerSingleton("otherConnectionDetails", (OtherConnectionDetails) () -> {});

        registerSharedDevService(registry);

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void whenConnectionDetailsFactoryReturnsWrongTypeThenManagedContainerIsRegistered() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.FRAMEWORK)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                // Raw type to bypass the compile-time check and exercise the runtime net.
                .discovery((Class) OtherConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host())));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenNoSharedContainerAvailableThenManagedContainerIsRegistered() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(beanFactory, null);

        registerSharedDevService(registry);

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenNoneStrategyThenNoDiscoveryHappens() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .discovery(TestConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host())));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenNotInDevModeThenNoDiscoveryHappens() {
        enableTestMode();
        DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

        registerSharedDevService(registry);

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isTrue();
        assertThat(beanFactory.containsBeanDefinition("devService.connectionDetails.postgres")).isFalse();
    }

    @Test
    void whenConnectionDetailsFactoryFailsThenManagedContainerIsRegistered() {
        enableDevMode();
        DevServicesRegistry registry = registryDiscovering(beanFactory, discoveredContainer());

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
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
    void whenOldestDiscoveredContainerIsUnusableThenNextOneIsUsed() {
        enableDevMode();
        DiscoveredContainer unusable = new DiscoveredContainer(ContainerInfo.builder()
                .id("def456")
                .imageName("postgres:latest")
                .names(List.of("unusable-postgres"))
                .labels(Map.of(DevServiceLabels.NAME, "postgres", DevServiceLabels.DISCOVERABLE, "true"))
                .state("running")
                .status("running")
                .build(), "localhost");
        DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment(),
                serviceName -> List.of(unusable, discoveredContainer()));

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.FRAMEWORK)
                .container(TestPostgresContainer.class, TestPostgresContainer::new)
                .discovery(TestConnectionDetails.class,
                        container -> new TestConnectionDetails(container.host() + ":" + container.mappedPort(5432))));

        assertThat(beanFactory.containsBeanDefinition("devService.container.postgres")).isFalse();
        assertThat(beanFactory.getBean("devService.connectionDetails.postgres", TestConnectionDetails.class).host())
                .isEqualTo("localhost:54321");
    }

}
