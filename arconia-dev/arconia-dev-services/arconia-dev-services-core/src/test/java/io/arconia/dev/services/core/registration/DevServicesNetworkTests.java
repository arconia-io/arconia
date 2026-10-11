package io.arconia.dev.services.core.registration;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import io.arconia.dev.services.core.TestDevServicesProperties;
import io.arconia.dev.services.core.container.DevServiceContainerCustomizer;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestNetwork;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.enableDevMode;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.networkEnabledEnvironment;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerNetworkBean;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerPostgresDevService;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registryWithNetworkEnabled;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.resetBootstrapMode;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesRegistry}: the shared network dev service containers join.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServicesNetworkTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    @BeforeEach
    @AfterEach
    void reset() {
        resetBootstrapMode();
    }

    @Test
    void whenNetworkEnabledThenContainerJoinsNetworkWithServiceNameAlias() {
        DevServicesRegistry registry = registryWithNetworkEnabled(beanFactory);
        TestNetwork network = new TestNetwork("net-1");
        registerNetworkBean(beanFactory, network);

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, () -> new TestPostgresContainer().withNetworkAliases("db")));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(network);
        // The service name is added next to the user-defined alias.
        assertThat(container.getNetworkAliases()).contains("db", "postgres");
    }

    @Test
    void whenNetworkDisabledThenContainerDoesNotJoinNetwork() {
        DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment());
        registerNetworkBean(beanFactory, new TestNetwork("net-1"));

        // The environment has no network.enabled property, so the network is off.
        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, () -> new TestPostgresContainer().withNetworkAliases("db")));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isNull();
        assertThat(container.getNetworkAliases()).doesNotContain("postgres");
    }

    @Test
    void whenNetworkEnabledButNoNetworkBeanThenContainerJoinsTheTestcontainersNetwork() {
        // The network bean comes from the global auto-configuration, which a context importing
        // only a dev service auto-configuration (e.g. a test slice) may lack.
        DevServicesRegistry registry = registryWithNetworkEnabled(beanFactory);
        registerPostgresDevService(registry);

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(Network.SHARED);
        assertThat(container.getNetworkAliases()).contains("postgres");
    }

    @Test
    void whenNetworkAlreadySetByCustomizerThenItIsHonored() {
        DevServicesRegistry registry = registryWithNetworkEnabled(beanFactory);
        TestNetwork beanNetwork = new TestNetwork("net-bean");
        TestNetwork customizerNetwork = new TestNetwork("net-customizer");
        registerNetworkBean(beanFactory, beanNetwork);
        beanFactory.registerSingleton("networkCustomizer",
                (DevServiceContainerCustomizer<GenericContainer<?>>) container -> container.withNetwork(customizerNetwork));

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPostgresContainer.class, () -> new TestPostgresContainer().withNetworkAliases("db")));

        var container = beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(container.getNetwork()).isSameAs(customizerNetwork);
    }

    @Test
    void whenContainerReusedOnTheSharedNetworkThenWarns(CapturedOutput output) {
        enableDevMode();
        DevServicesRegistry registry = new DevServicesRegistry(beanFactory, networkEnabledEnvironment(), serviceName -> List.of(), () -> true);
        registerNetworkBean(beanFactory, Network.SHARED);

        registry.registerDevService(service -> service
                .name("postgres")
                .description("Test Dev Service")
                .properties(TestDevServicesProperties.TESTCONTAINERS)
                .container(TestPostgresContainer.class, TestPostgresContainer::new));

        beanFactory.getBean("devService.container.postgres", GenericContainer.class);

        assertThat(output).contains("doesn't combine with the shared network").contains("postgres");
    }

}
