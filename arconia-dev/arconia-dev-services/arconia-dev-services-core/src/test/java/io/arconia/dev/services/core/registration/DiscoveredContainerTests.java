package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.TestSslDevServicesProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link DiscoveredContainer}.
 */
class DiscoveredContainerTests {

    @Test
    void whenContainerInfoIsNullThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DiscoveredContainer.builder().host("localhost").build())
                .withMessageContaining("containerInfo cannot be null");
    }

    @Test
    void whenHostIsEmptyThenThrow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> DiscoveredContainer.builder().containerInfo(containerInfo()).host("").build())
                .withMessageContaining("host cannot be null or empty");
    }

    @Test
    void whenContainerPortIsMappedThenReturnHostPort() {
        var container = discovered(containerInfo());

        assertThat(container.mappedPort(5432)).isEqualTo(54321);
    }

    @Test
    void whenContainerPortIsNotMappedThenThrow() {
        var container = discovered(containerInfo());

        assertThatIllegalStateException()
                .isThrownBy(() -> container.mappedPort(8080))
                .withMessageContaining("No host port mapped to container port 8080");
    }

    @Test
    void whenContainerPortIsMappedForMultipleIpFamiliesThenIpv4BindingWins() {
        var container = DiscoveredContainer.builder().containerInfo(containerInfo(List.of(
                new ContainerInfo.ContainerPort("::", 5432, 54322, "tcp"),
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp")))).host("localhost").build();

        assertThat(container.mappedPort(5432)).isEqualTo(54321);
    }

    @Test
    void whenContainerPortIsMappedForBothTcpAndUdpThenTcpMappingWins() {
        var container = DiscoveredContainer.builder().containerInfo(containerInfo(List.of(
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54322, "udp"),
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp")))).host("localhost").build();

        assertThat(container.mappedPort(5432)).isEqualTo(54321);
    }

    @Test
    void whenContainerPortIsMappedOnlyOverUdpThenThrow() {
        var container = discovered(containerInfo(List.of(
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "udp"))));

        assertThatIllegalStateException()
                .isThrownBy(() -> container.mappedPort(5432))
                .withMessageContaining("No host port mapped to container port 5432");
    }

    @Test
    void whenNoBundleIsGivenThenContainerIsConnectedToWithoutSsl() {
        assertThat(discovered(containerInfo()).sslBundle()).isNull();
    }

    @Test
    void whenBundleIsGivenThenContainerCarriesIt() {
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getTruststore().setCertificate(TestSslDevServicesProperties.CA_CERTIFICATE);
        SslBundle sslBundle = PropertiesSslBundle.get(properties);

        var container = DiscoveredContainer.builder().containerInfo(containerInfo()).host("localhost").sslBundle(sslBundle).build();

        assertThat(container.sslBundle()).isSameAs(sslBundle);
    }

    @Test
    void whenContainerPathIsEmptyThenThrow() {
        var container = discovered(containerInfo());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> container.copyFileFromContainer(""))
                .withMessageContaining("containerPath cannot be null or empty");
    }

    private static DiscoveredContainer discovered(ContainerInfo containerInfo) {
        return DiscoveredContainer.builder().containerInfo(containerInfo).host("localhost").build();
    }

    private static ContainerInfo containerInfo() {
        return containerInfo(List.of(
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 54321, "tcp"),
                new ContainerInfo.ContainerPort(null, 9090, null, "tcp")));
    }

    private static ContainerInfo containerInfo(List<ContainerInfo.ContainerPort> ports) {
        return ContainerInfo.builder()
                .id("abc123")
                .imageName("postgres:latest")
                .names(List.of("shared-postgres"))
                .ports(ports)
                .labels(Map.of())
                .state("running")
                .status("running")
                .build();
    }

}
