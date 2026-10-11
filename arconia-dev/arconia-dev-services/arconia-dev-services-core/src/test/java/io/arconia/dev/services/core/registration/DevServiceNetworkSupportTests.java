package io.arconia.dev.services.core.registration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.Network;

import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestNetwork;
import io.arconia.dev.services.core.registration.DevServicesTestSupport.TestPostgresContainer;

import static io.arconia.dev.services.core.registration.DevServicesTestSupport.networkEnabledEnvironment;
import static io.arconia.dev.services.core.registration.DevServicesTestSupport.registerNetworkBean;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServiceNetworkSupport}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevServiceNetworkSupportTests {

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServiceNetworkSupport enabledSupport = new DevServiceNetworkSupport(networkEnabledEnvironment());

    private final DevServiceNetworkSupport disabledSupport = new DevServiceNetworkSupport(new StandardEnvironment());

    @Test
    void whenPropertyIsNotSetThenNetworkIsDisabled() {
        assertThat(disabledSupport.isEnabled()).isFalse();
    }

    @Test
    void whenPropertyIsSetThenNetworkIsEnabled() {
        assertThat(enabledSupport.isEnabled()).isTrue();
    }

    @Test
    void whenEnabledThenContainerJoinsNetworkBeanWithServiceNameAlias() {
        TestNetwork network = new TestNetwork("net-1");
        registerNetworkBean(beanFactory, network);
        var container = new TestPostgresContainer().withNetworkAliases("db");

        enabledSupport.join(container, "postgres", beanFactory);

        assertThat(container.getNetwork()).isSameAs(network);
        // The service name is added next to the user-defined alias.
        assertThat(container.getNetworkAliases()).contains("db", "postgres");
    }

    @Test
    void whenEnabledButNoNetworkBeanThenContainerJoinsTheTestcontainersNetwork() {
        // The network bean comes from the global auto-configuration, which a context importing
        // only a dev service auto-configuration (e.g. a test slice) may lack.
        var container = new TestPostgresContainer();

        enabledSupport.join(container, "postgres", beanFactory);

        assertThat(container.getNetwork()).isSameAs(Network.SHARED);
        assertThat(container.getNetworkAliases()).contains("postgres");
    }

    @Test
    void whenDisabledThenContainerIsLeftUntouched() {
        registerNetworkBean(beanFactory, new TestNetwork("net-1"));
        var container = new TestPostgresContainer().withNetworkAliases("db");

        disabledSupport.join(container, "postgres", beanFactory);

        assertThat(container.getNetwork()).isNull();
        assertThat(container.getNetworkAliases()).contains("db").doesNotContain("postgres");
    }

    @Test
    void whenNetworkAlreadySetThenItIsHonored() {
        registerNetworkBean(beanFactory, new TestNetwork("net-bean"));
        TestNetwork ownNetwork = new TestNetwork("net-own");
        var container = new TestPostgresContainer().withNetwork(ownNetwork);

        enabledSupport.join(container, "postgres", beanFactory);

        assertThat(container.getNetwork()).isSameAs(ownNetwork);
        assertThat(container.getNetworkAliases()).contains("postgres");
    }

    @Test
    void whenContainerIsReusableThenWarns(CapturedOutput output) {
        var container = new TestPostgresContainer().withReuse(true);

        enabledSupport.join(container, "postgres", beanFactory);

        assertThat(output).contains("doesn't combine with the shared network").contains("postgres");
    }

}
