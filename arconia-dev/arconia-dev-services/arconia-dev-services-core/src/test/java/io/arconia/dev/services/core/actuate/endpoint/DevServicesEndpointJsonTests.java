package io.arconia.dev.services.core.actuate.endpoint;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.actuate.endpoint.DevServicesEndpoint.ServiceInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the JSON the {@link DevServicesEndpoint} produces, which is the contract the
 * documentation shows and developer tooling reads.
 */
class DevServicesEndpointJsonTests {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    @SuppressWarnings("unchecked")
    void serviceInfoSerializesAsDocumented() {
        ContainerInfo containerInfo = ContainerInfo.builder()
                .id("cbf5a15cf542")
                .imageName("postgres:18")
                .names(List.of("unwritten_mithrandir"))
                .exposedPorts(List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5432, 36803, "tcp")))
                .labels(Map.of("io.arconia.dev-service.name", "postgresql"))
                .state("running")
                .status("Up About 67 minutes")
                .build();
        ServiceInfo serviceInfo = new ServiceInfo("postgresql", "PostgreSQL Dev Service", DevServiceRegistration.Origin.MANAGED,
                List.of(new DevServiceLink("Console", "http://localhost:36803/console")), containerInfo);

        Map<String, Object> json = jsonMapper.readValue(jsonMapper.writeValueAsString(serviceInfo), Map.class);

        assertThat(json).containsOnlyKeys("name", "description", "origin", "links", "containerInfo");
        assertThat(json).containsEntry("name", "postgresql").containsEntry("description", "PostgreSQL Dev Service").containsEntry("origin", "MANAGED");
        assertThat((List<Map<String, Object>>) json.get("links")).singleElement().satisfies(link ->
                assertThat(link).containsOnly(Map.entry("label", "Console"), Map.entry("url", "http://localhost:36803/console")));
        Map<String, Object> container = (Map<String, Object>) json.get("containerInfo");
        assertThat(container).containsOnlyKeys("id", "imageName", "names", "exposedPorts", "labels", "state", "status");
        assertThat(container).containsEntry("id", "cbf5a15cf542").containsEntry("imageName", "postgres:18")
                .containsEntry("names", List.of("unwritten_mithrandir"))
                .containsEntry("state", "running").containsEntry("status", "Up About 67 minutes");
        assertThat((List<Map<String, Object>>) container.get("exposedPorts")).singleElement().satisfies(port ->
                assertThat(port).containsOnly(Map.entry("ip", "0.0.0.0"), Map.entry("privatePort", 5432),
                        Map.entry("publicPort", 36803), Map.entry("type", "tcp")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void containerInfoIsNullWhenTheContainerCannotBeInspected() {
        ServiceInfo serviceInfo = new ServiceInfo("postgresql", "PostgreSQL Dev Service", DevServiceRegistration.Origin.DISCOVERED, List.of(), null);

        Map<String, Object> json = jsonMapper.readValue(jsonMapper.writeValueAsString(serviceInfo), Map.class);

        assertThat(json).containsEntry("origin", "DISCOVERED").containsEntry("links", List.of());
        assertThat(json).containsKey("containerInfo");
        assertThat(json.get("containerInfo")).isNull();
    }

}
