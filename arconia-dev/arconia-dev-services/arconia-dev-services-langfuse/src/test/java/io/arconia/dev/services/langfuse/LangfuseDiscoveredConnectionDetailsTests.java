package io.arconia.dev.services.langfuse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.testcontainers.langfuse.LangfuseContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for the connection details of a Langfuse dev service running in a container
 * discovered from another application.
 */
class LangfuseDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int MAPPED_PORT = 49152;

    @Test
    void connectionDetailsUseTheApiKeyProperties() {
        var properties = new LangfuseDevServicesProperties();
        properties.setPublicKey("property-public");
        properties.setSecretKey("property-secret");

        var connectionDetails = new LangfuseDiscoveredConnectionDetails(discoveredContainer(), properties);

        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF))
                .isEqualTo("http://docker.host:49152" + LangfuseContainer.OTLP_TRACES_PATH);
        assertThat(connectionDetails.getHeaders())
                .containsEntry("Authorization", basicAuth("property-public", "property-secret"))
                .containsEntry("x-langfuse-ingestion-version", "4");
    }

    @Test
    void connectionDetailsPreferTheApiKeysSetThroughTheEnvironment() {
        var properties = new LangfuseDevServicesProperties();
        properties.setPublicKey("property-public");
        properties.setSecretKey("property-secret");
        properties.setEnvironment(Map.of(
                "LANGFUSE_INIT_PROJECT_PUBLIC_KEY", "environment-public",
                "LANGFUSE_INIT_PROJECT_SECRET_KEY", "environment-secret"));

        var connectionDetails = new LangfuseDiscoveredConnectionDetails(discoveredContainer(), properties);

        assertThat(connectionDetails.getHeaders())
                .containsEntry("Authorization", basicAuth("environment-public", "environment-secret"));
    }

    @Test
    void connectionDetailsRejectGrpc() {
        var connectionDetails = new LangfuseDiscoveredConnectionDetails(discoveredContainer(), new LangfuseDevServicesProperties());

        assertThatIllegalStateException()
                .isThrownBy(() -> connectionDetails.getTracesUrl(Protocol.GRPC))
                .withMessageContaining("Langfuse doesn't support OTLP over gRPC");
    }

    private static String basicAuth(String publicKey, String secretKey) {
        return "Basic " + Base64.getEncoder().encodeToString((publicKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));
    }

    private static DiscoveredContainer discoveredContainer() {
        return DiscoveredContainer.builder().containerInfo(ContainerInfo.builder()
                .id("abc123")
                .imageName("langfuse/langfuse:latest")
                .ports(List.of(new ContainerInfo.ContainerPort("0.0.0.0", LangfuseContainer.HTTP_PORT, MAPPED_PORT, "tcp")))
                .state("running")
                .status("running")
                .build()).host(HOST).build();
    }

}
