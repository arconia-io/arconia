package io.arconia.dev.services.opentelemetry.collector;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtelCollectorDiscoveredConnectionDetails}.
 */
class OtelCollectorDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int HTTP_MAPPED_PORT = 49152;

    private static final int GRPC_MAPPED_PORT = 49153;

    @Test
    void connectionDetailsUseHttpWithoutSsl() {
        var connectionDetails = new OtelCollectorDiscoveredConnectionDetails(discoveredContainer(null));

        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).isEqualTo("http://docker.host:49152/v1/traces");
        assertThat(connectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF)).isEqualTo("http://docker.host:49152/v1/metrics");
        assertThat(connectionDetails.getLogsUrl(Protocol.GRPC)).isEqualTo("http://docker.host:49153");
        assertThat(connectionDetails.getSslBundle()).isNull();
    }

    @Test
    void connectionDetailsUseHttpsWithSsl() {
        SslBundle sslBundle = SslBundle.of(null);
        var connectionDetails = new OtelCollectorDiscoveredConnectionDetails(discoveredContainer(sslBundle));

        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).isEqualTo("https://docker.host:49152/v1/traces");
        assertThat(connectionDetails.getMetricsUrl(Protocol.HTTP_PROTOBUF)).isEqualTo("https://docker.host:49152/v1/metrics");
        assertThat(connectionDetails.getLogsUrl(Protocol.GRPC)).isEqualTo("https://docker.host:49153");
        assertThat(connectionDetails.getSslBundle()).isSameAs(sslBundle);
    }

    private static DiscoveredContainer discoveredContainer(@Nullable SslBundle sslBundle) {
        return DiscoveredContainer.builder().containerInfo(ContainerInfo.builder()
                .id("abc123")
                .imageName("otel/opentelemetry-collector-contrib:latest")
                .ports(List.of(
                        new ContainerInfo.ContainerPort("0.0.0.0", ArconiaOtelCollectorContainer.OTLP_HTTP_PORT, HTTP_MAPPED_PORT, "tcp"),
                        new ContainerInfo.ContainerPort("0.0.0.0", ArconiaOtelCollectorContainer.OTLP_GRPC_PORT, GRPC_MAPPED_PORT, "tcp")))
                .state("running")
                .status("running")
                .build()).host(HOST).sslBundle(sslBundle).build();
    }

}
