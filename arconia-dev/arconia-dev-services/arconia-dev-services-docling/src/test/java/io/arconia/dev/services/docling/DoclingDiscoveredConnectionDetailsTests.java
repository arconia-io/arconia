package io.arconia.dev.services.docling;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.core.registration.DiscoveredContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the connection details of a Docling dev service running in a container
 * discovered from another application.
 */
class DoclingDiscoveredConnectionDetailsTests {

    private static final String HOST = "docker.host";

    private static final int MAPPED_PORT = 49152;

    @Test
    void connectionDetailsUseTheApiKeyProperty() {
        var properties = new DoclingDevServicesProperties();
        properties.setApiKey("property-key");

        var connectionDetails = new DoclingDiscoveredConnectionDetails(discoveredContainer(), properties);

        assertThat(connectionDetails.getBaseUrl()).hasToString("http://docker.host:49152");
        assertThat(connectionDetails.getApiKey()).isEqualTo("property-key");
    }

    @Test
    void connectionDetailsPreferTheApiKeySetThroughTheEnvironment() {
        var properties = new DoclingDevServicesProperties();
        properties.setApiKey("property-key");
        properties.setEnvironment(Map.of("DOCLING_SERVE_API_KEY", "environment-key"));

        var connectionDetails = new DoclingDiscoveredConnectionDetails(discoveredContainer(), properties);

        assertThat(connectionDetails.getApiKey()).isEqualTo("environment-key");
    }

    private static DiscoveredContainer discoveredContainer() {
        return DiscoveredContainer.builder().containerInfo(ContainerInfo.builder()
                .id("abc123")
                .imageName("docling-serve:latest")
                .ports(List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5001, MAPPED_PORT, "tcp")))
                .state("running")
                .status("running")
                .build()).host(HOST).build();
    }

}
