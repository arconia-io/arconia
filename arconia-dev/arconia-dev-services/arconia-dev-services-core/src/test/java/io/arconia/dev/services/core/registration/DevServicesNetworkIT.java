package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.UUID;

import com.github.dockerjava.api.DockerClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.core.container.DevServiceLabels;
import io.arconia.dev.services.core.container.DevServicesNetworkFactory;

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

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
        BootstrapMode.clear();
    }

    @Test
    void networkedContainersReachEachOtherByAlias() throws Exception {
        Network network = DevServicesNetworkFactory.resolve(null);
        registerNetworkBean(network);

        registry.registerDevService(service -> service
                .name("peer-a")
                .container(container -> container
                        .type(TestPeerContainer.class)
                        .supplier(() -> new TestPeerContainer().withNetworkAliases("peer-a"))
                        .serviceConnectionName(null))
                .network(net -> net.enabled(true)));
        registry.registerDevService(service -> service
                .name("peer-b")
                .container(container -> container
                        .type(TestPeerContainer.class)
                        .supplier(() -> new TestPeerContainer().withNetworkAliases("peer-b"))
                        .serviceConnectionName(null))
                .network(net -> net.enabled(true)));

        var peerA = beanFactory.getBean("devService.container.peer-a", TestPeerContainer.class);
        var peerB = beanFactory.getBean("devService.container.peer-b", TestPeerContainer.class);

        // Both containers joined the same network and recorded their user-defined alias.
        assertThat(peerA.getNetwork()).isSameAs(network);
        assertThat(peerB.getNetwork()).isSameAs(network);
        assertThat(peerA.getLabels()).containsEntry(DevServiceLabels.NETWORK_ALIASES, "peer-a");
        assertThat(peerB.getLabels()).containsEntry(DevServiceLabels.NETWORK_ALIASES, "peer-b");

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
    void whenPeerDoesNotJoinNetworkThenAliasDoesNotResolve() throws Exception {
        Network network = DevServicesNetworkFactory.resolve(null);
        registerNetworkBean(network);

        registry.registerDevService(service -> service
                .name("peer-a")
                .container(container -> container
                        .type(TestPeerContainer.class)
                        .supplier(() -> new TestPeerContainer().withNetworkAliases("peer-a"))
                        .serviceConnectionName(null))
                .network(net -> net.enabled(true)));
        // peer-b does not join the network (no network spec), so it stays on the default bridge.
        registry.registerDevService(service -> service
                .name("peer-b")
                .container(container -> container
                        .type(TestPeerContainer.class)
                        .supplier(() -> new TestPeerContainer().withNetworkAliases("peer-b"))
                        .serviceConnectionName(null)));

        var peerA = beanFactory.getBean("devService.container.peer-a", TestPeerContainer.class);
        var peerB = beanFactory.getBean("devService.container.peer-b", TestPeerContainer.class);

        assertThat(peerA.getNetwork()).isSameAs(network);
        assertThat(peerB.getNetwork()).isNull();

        try {
            peerB.start();
            peerA.start();

            // Negative control: peer-b is not on the shared network, so its alias must not resolve
            // from peer-a, confirming that alias resolution requires joining the shared network (the
            // positive counterpart is networkedContainersReachEachOtherByAlias).
            var result = peerA.execInContainer("wget", "-q", "-T", "5", "-O", "-", "http://peer-b");

            assertThat(result.getExitCode()).isNotZero();
        }
        finally {
            peerA.stop();
            peerB.stop();
        }
    }

    @Test
    void namedNetworkIsFoundOrCreatedAndReused() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "dev");
        BootstrapMode.clear();
        String networkName = "arconia-network-it-" + UUID.randomUUID();
        DockerClient client = DockerClientFactory.lazyClient();
        String createdId = null;
        try {
            Network first = DevServicesNetworkFactory.resolve(networkName);
            createdId = first.getId();
            assertThat(createdId).isNotBlank();

            // The Docker network was actually created with the requested name.
            assertThat(networksNamed(client, networkName))
                    .singleElement()
                    .satisfies(id -> assertThat(id).isEqualTo(first.getId()));

            // Resolving again finds the existing network instead of creating a second one.
            Network second = DevServicesNetworkFactory.resolve(networkName);
            assertThat(second.getId()).isEqualTo(createdId);
            assertThat(networksNamed(client, networkName)).hasSize(1);
        }
        finally {
            if (createdId != null) {
                try {
                    client.removeNetworkCmd(createdId).exec();
                }
                catch (Exception ignored) {
                    // Best-effort cleanup; the factory's shutdown hook is the backstop.
                }
            }
        }
    }

    private static List<String> networksNamed(DockerClient client, String name) {
        return client.listNetworksCmd().withNameFilter(name).exec().stream()
                .filter(network -> name.equals(network.getName()))
                .map(com.github.dockerjava.api.model.Network::getId)
                .toList();
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
