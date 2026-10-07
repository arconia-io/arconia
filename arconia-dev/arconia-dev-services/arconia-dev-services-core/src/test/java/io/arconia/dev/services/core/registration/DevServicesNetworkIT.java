package io.arconia.dev.services.core.registration;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the shared network support: two dev service containers that opt in
 * to the network can reach each other by their network alias.
 */
@EnabledIfDockerAvailable
class DevServicesNetworkIT {

    private static final DockerImageName NGINX_IMAGE = DockerImageName.parse("nginx:alpine3.24");

    private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

    private final DevServicesRegistry registry = new DevServicesRegistry(beanFactory, new StandardEnvironment());

    @Test
    void networkedContainersReachEachOtherByAlias() throws Exception {
        DevServicesRegistry registry = registryWithNetworkEnabled();
        Network network = Network.SHARED;
        registerNetworkBean(network);

        registry.registerDevService(service -> service
                .name("peer-a")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPeerContainer.class, () -> new TestPeerContainer().withNetworkAliases("peer-a"))
                        .serviceConnection(false));
        registry.registerDevService(service -> service
                .name("peer-b")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPeerContainer.class, () -> new TestPeerContainer().withNetworkAliases("peer-b"))
                        .serviceConnection(false));

        var peerA = beanFactory.getBean("devService.container.peer-a", TestPeerContainer.class);
        var peerB = beanFactory.getBean("devService.container.peer-b", TestPeerContainer.class);

        // Both containers joined the same network.
        assertThat(peerA.getNetwork()).isSameAs(network);
        assertThat(peerB.getNetwork()).isSameAs(network);

        try {
            peerB.start();
            peerA.start();

            // Peer A reaches peer B over the shared network by its alias (which only resolves
            // on a user-defined network, never on the default bridge). The probe retries to absorb
            // the brief window before the peer's server is reachable by its alias.
            var result = peerA.execInContainer("sh", "-c",
                    "for i in 1 2 3 4 5; do wget -q -T 5 -O - http://peer-b && exit 0; sleep 1; done; exit 1");

            assertThat(result.getExitCode()).isZero();
            assertThat(result.getStdout()).containsIgnoringCase("nginx");
        }
        finally {
            peerA.stop();
            peerB.stop();
        }
    }

    @Test
    void whenNetworkDisabledThenAliasDoesNotResolve() throws Exception {
        // The default registry has no network.enabled property, so both containers stay on the
        // default bridge and cannot reach each other by network alias.
        registry.registerDevService(service -> service
                .name("peer-a")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPeerContainer.class, () -> new TestPeerContainer().withNetworkAliases("peer-a"))
                        .serviceConnection(false));
        registry.registerDevService(service -> service
                .name("peer-b")
                .properties(TestDevServicesProperties.DEFAULT)
                .container(TestPeerContainer.class, () -> new TestPeerContainer().withNetworkAliases("peer-b"))
                        .serviceConnection(false));

        var peerA = beanFactory.getBean("devService.container.peer-a", TestPeerContainer.class);
        var peerB = beanFactory.getBean("devService.container.peer-b", TestPeerContainer.class);

        assertThat(peerA.getNetwork()).isNull();
        assertThat(peerB.getNetwork()).isNull();

        try {
            peerB.start();
            peerA.start();

            // Negative control: with the shared network disabled, peer-b's alias must not resolve
            // from peer-a, confirming that alias resolution requires the shared network (the positive
            // counterpart is networkedContainersReachEachOtherByAlias).
            var result = peerA.execInContainer("wget", "-q", "-T", "5", "-O", "-", "http://peer-b");

            assertThat(result.getExitCode()).isNotZero();
        }
        finally {
            peerA.stop();
            peerB.stop();
        }
    }

    private DevServicesRegistry registryWithNetworkEnabled() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test-network",
                Map.of("arconia.dev.services.network.enabled", "true")));
        return new DevServicesRegistry(beanFactory, environment);
    }

    private void registerNetworkBean(Network network) {
        var beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(network.getClass());
        beanDefinition.setInstanceSupplier(() -> network);
        beanFactory.registerBeanDefinition("devServicesNetwork", beanDefinition);
    }

    static class TestPeerContainer extends GenericContainer<TestPeerContainer> {
        TestPeerContainer() {
            super(NGINX_IMAGE);
            // Expose the HTTP port so the default wait strategy blocks until the server is
            // listening, rather than returning as soon as the container is merely running.
            withExposedPorts(80);
        }
    }

}
