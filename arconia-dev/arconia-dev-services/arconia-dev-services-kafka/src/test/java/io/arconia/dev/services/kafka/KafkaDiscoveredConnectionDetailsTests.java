package io.arconia.dev.services.kafka;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.registration.DiscoveredContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KafkaDiscoveredConnectionDetails}.
 */
class KafkaDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int MAPPED_PORT = 49152;

    @Test
    void connectionDetailsUsePlaintextWithoutSsl() {
        var connectionDetails = new KafkaDiscoveredConnectionDetails(discoveredContainer(null));

        assertThat(connectionDetails.getBootstrapServers()).containsExactly("docker.host:49152");
        assertThat(connectionDetails.getSecurityProtocol()).isEqualTo("PLAINTEXT");
        assertThat(connectionDetails.getSslBundle()).isNull();
    }

    @Test
    void connectionDetailsUseSslWithSsl() {
        SslBundle sslBundle = SslBundle.of(null);
        var connectionDetails = new KafkaDiscoveredConnectionDetails(discoveredContainer(sslBundle));

        assertThat(connectionDetails.getBootstrapServers()).containsExactly("docker.host:49152");
        assertThat(connectionDetails.getSecurityProtocol()).isEqualTo("SSL");
        assertThat(connectionDetails.getSslBundle()).isSameAs(sslBundle);
    }

    private static DiscoveredContainer discoveredContainer(@Nullable SslBundle sslBundle) {
        return DiscoveredContainer.builder().containerInfo(ContainerInfo.builder()
                .id("abc123")
                .imageName("apache/kafka-native:latest")
                .ports(List.of(new ContainerInfo.ContainerPort("0.0.0.0", KafkaDevServicesAutoConfiguration.KAFKA_PORT, MAPPED_PORT, "tcp")))
                .state("running")
                .status("running")
                .build()).host(HOST).sslBundle(sslBundle).build();
    }

}
