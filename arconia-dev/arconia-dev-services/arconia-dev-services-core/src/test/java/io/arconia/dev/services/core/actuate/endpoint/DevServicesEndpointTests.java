package io.arconia.dev.services.core.actuate.endpoint;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.actuate.endpoint.DevServicesEndpoint.ServiceInfo;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesEndpoint}.
 */
class DevServicesEndpointTests {

    private final Map<String, DevServiceRegistration> registrations = new LinkedHashMap<>();

    @Test
    void devServicesReturnsEmptyMapWhenNoRegistrations() {
        Map<String, ServiceInfo> result = endpoint().devServices();

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void endpointDefensivelyCopiesRegistrationsAtConstruction() {
        DevServicesEndpoint endpoint = endpoint();
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> containerInfo("container-1", "postgres:18")));

        assertThat(endpoint.devServices()).isEmpty();
    }

    @Test
    void devServicesReturnsMultipleServicesInRegistrationOrder() {
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> containerInfo("container-1", "postgres:18")));
        registrations.put("docling", registration("docling", "Docling Serve", () -> containerInfo("container-2", "docling:1.10")));
        registrations.put("artemis", registration("artemis", "Artemis Broker", () -> containerInfo("container-3", "artemis:2.40")));

        Map<String, ServiceInfo> result = endpoint().devServices();

        assertThat(result.keySet()).containsExactly("postgres", "docling", "artemis");
        assertThat(result.get("postgres").description()).isEqualTo("PostgreSQL Database");
        assertThat(result.get("postgres").containerInfo().imageName()).isEqualTo("postgres:18");
        assertThat(result.get("docling").description()).isEqualTo("Docling Serve");
        assertThat(result.get("docling").containerInfo().imageName()).isEqualTo("docling:1.10");
    }

    @Test
    void devServiceReturnsServiceInfoWhenServiceExists() {
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> containerInfo("container-1", "postgres:18")));

        ServiceInfo result = endpoint().devService("postgres");

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("postgres");
        assertThat(result.description()).isEqualTo("PostgreSQL Database");
        assertThat(result.origin()).isEqualTo(DevServiceRegistration.Origin.MANAGED);
        assertThat(result.containerInfo()).isNotNull();
        assertThat(result.containerInfo().id()).isEqualTo("container-1");
        assertThat(result.containerInfo().imageName()).isEqualTo("postgres:18");
        assertThat(result.containerInfo().state()).isEqualTo("running");
        assertThat(result.containerInfo().status()).isEqualTo("Up 5 minutes");
        assertThat(result.containerInfo().names()).containsExactly("postgres-container");
        assertThat(result.containerInfo().labels()).containsEntry("app", "test");
        assertThat(result.containerInfo().ports()).hasSize(1);
    }

    @Test
    void devServiceReturnsNullWhenServiceNotFound() {
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> containerInfo("container-1", "postgres:18")));

        assertThat(endpoint().devService("docling")).isNull();
    }

    @Test
    void containerInfoIsNullWhenContainerCannotBeInspected() {
        registrations.put("postgres", registration("postgres", "PostgreSQL Database", () -> {
            throw new IllegalStateException("Container not found");
        }));

        assertThat(endpoint().devServices().get("postgres").containerInfo()).isNull();
        assertThat(endpoint().devService("postgres").containerInfo()).isNull();
    }

    private DevServicesEndpoint endpoint() {
        return new DevServicesEndpoint(registrations);
    }

    private static DevServiceRegistration registration(String name, String description, Supplier<ContainerInfo> containerInfo) {
        return new DevServiceRegistration(name, description, DevServiceRegistration.Origin.MANAGED, containerInfo, List.of());
    }

    private static ContainerInfo containerInfo(String id, String imageName) {
        return new ContainerInfo(id, imageName, List.of("postgres-container"),
                List.of(new ContainerInfo.ContainerPort("0.0.0.0", 5432, 5432, "tcp")), Map.of("app", "test"), "running", "Up 5 minutes");
    }

}
