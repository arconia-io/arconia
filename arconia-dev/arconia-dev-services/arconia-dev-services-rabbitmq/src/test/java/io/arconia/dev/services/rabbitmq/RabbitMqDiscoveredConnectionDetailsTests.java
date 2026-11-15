package io.arconia.dev.services.rabbitmq;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.registration.DiscoveredContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RabbitMqDiscoveredConnectionDetails}.
 */
class RabbitMqDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int AMQP_MAPPED_PORT = 49152;

    private static final int AMQPS_MAPPED_PORT = 49153;

    @Test
    void connectionDetailsUseTheAmqpPortWithoutSsl() {
        var properties = new RabbitMqDevServicesProperties();
        properties.setUsername("user");
        properties.setPassword("secret");

        var connectionDetails = new RabbitMqDiscoveredConnectionDetails(discoveredContainer(null), properties);

        assertThat(connectionDetails.getAddresses()).singleElement().satisfies(address -> {
            assertThat(address.host()).isEqualTo(HOST);
            assertThat(address.port()).isEqualTo(AMQP_MAPPED_PORT);
        });
        assertThat(connectionDetails.getUsername()).isEqualTo("user");
        assertThat(connectionDetails.getPassword()).isEqualTo("secret");
        assertThat(connectionDetails.getSslBundle()).isNull();
    }

    @Test
    void connectionDetailsUseTheAmqpsPortWithSsl() {
        SslBundle sslBundle = SslBundle.of(null);

        var connectionDetails = new RabbitMqDiscoveredConnectionDetails(discoveredContainer(sslBundle), new RabbitMqDevServicesProperties());

        assertThat(connectionDetails.getAddresses()).singleElement().satisfies(address -> {
            assertThat(address.host()).isEqualTo(HOST);
            assertThat(address.port()).isEqualTo(AMQPS_MAPPED_PORT);
        });
        assertThat(connectionDetails.getSslBundle()).isSameAs(sslBundle);
    }

    private static DiscoveredContainer discoveredContainer(@Nullable SslBundle sslBundle) {
        return DiscoveredContainer.builder().containerInfo(ContainerInfo.builder()
                .id("abc123")
                .imageName("rabbitmq:latest")
                .ports(List.of(
                        new ContainerInfo.ContainerPort("0.0.0.0", RabbitMqDevServicesAutoConfiguration.AMQP_PORT, AMQP_MAPPED_PORT, "tcp"),
                        new ContainerInfo.ContainerPort("0.0.0.0", RabbitMqDevServicesAutoConfiguration.AMQPS_PORT, AMQPS_MAPPED_PORT, "tcp")))
                .state("running")
                .status("running")
                .build()).host(HOST).sslBundle(sslBundle).build();
    }

}
