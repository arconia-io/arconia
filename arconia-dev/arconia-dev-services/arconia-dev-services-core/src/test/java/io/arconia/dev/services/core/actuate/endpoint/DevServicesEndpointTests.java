package io.arconia.dev.services.core.actuate.endpoint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.core.actuate.endpoint.DevServicesEndpoint.ServiceInfo;
import io.arconia.dev.services.core.actuate.endpoint.DevServicesEndpoint.ServiceInfoSummary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesEndpoint}.
 */
class DevServicesEndpointTests {

    private Map<String, DevServiceRegistration> registrations;

    @BeforeEach
    void setUp() {
        registrations = new HashMap<>();
    }

    private DevServicesEndpoint endpoint() {
        return new DevServicesEndpoint(registrations);
    }

    @Test
    void devServicesReturnsEmptyMapWhenNoRegistrations() {
        Map<String, ServiceInfoSummary> result = endpoint().devServices();

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void endpointDefensivelyCopiesRegistrationsAtConstruction() {
        DevServicesEndpoint endpoint = endpoint();
        registrations.put("postgres", DevServiceRegistration.builder()
                .name("postgres")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> createContainerInfo("container-1", "postgres:18", "running"))
                .build());

        assertThat(endpoint.devServices()).isEmpty();
    }

    @Test
    void devServicesReturnsSingleServiceWhenOneRegistration() {
        ContainerInfo containerInfo = createContainerInfo("container-1", "postgres:18", "running");
        DevServiceRegistration registration = DevServiceRegistration.builder()
                .name("postgres")
                .description("PostgreSQL Database")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> containerInfo)
                .build();
        registrations.put("postgres", registration);

        Map<String, ServiceInfoSummary> result = endpoint().devServices();

        assertThat(result)
                .isNotNull()
                .hasSize(1)
                .containsKey("postgres");

        ServiceInfoSummary summary = result.get("postgres");
        assertThat(summary.name()).isEqualTo("postgres");
        assertThat(summary.description()).isEqualTo("PostgreSQL Database");
        assertThat(summary.origin()).isEqualTo(DevServiceRegistration.Origin.OWNED);
        assertThat(summary.containerInfo()).isNotNull();
        assertThat(summary.containerInfo().id()).isEqualTo("container-1");
        assertThat(summary.containerInfo().imageName()).isEqualTo("postgres:18");
        assertThat(summary.containerInfo().exposedPorts()).hasSize(1);
    }

    @Test
    void devServicesReturnsMultipleServicesWhenMultipleRegistrations() {
        ContainerInfo postgresContainer = createContainerInfo("container-1", "postgres:18", "running");
        ContainerInfo doclingContainer = createContainerInfo("container-2", "docling:1.10", "running");

        DevServiceRegistration postgresReg = DevServiceRegistration.builder()
                .name("postgres")
                .description("PostgreSQL Database")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> postgresContainer)
                .build();
        DevServiceRegistration doclingReg = DevServiceRegistration.builder()
                .name("docling")
                .description("Docling Serve")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> doclingContainer)
                .build();

        registrations.put("postgres", postgresReg);
        registrations.put("docling", doclingReg);

        Map<String, ServiceInfoSummary> result = endpoint().devServices();

        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .containsKeys("postgres", "docling");

        assertThat(result.get("postgres").name()).isEqualTo("postgres");
        assertThat(result.get("postgres").description()).isEqualTo("PostgreSQL Database");
        assertThat(result.get("postgres").containerInfo().imageName()).isEqualTo("postgres:18");

        assertThat(result.get("docling").name()).isEqualTo("docling");
        assertThat(result.get("docling").description()).isEqualTo("Docling Serve");
        assertThat(result.get("docling").containerInfo().imageName()).isEqualTo("docling:1.10");
    }

    @Test
    void devServicesHandlesNullDescription() {
        ContainerInfo containerInfo = createContainerInfo("container-1", "postgres:18", "running");
        DevServiceRegistration registration = DevServiceRegistration.builder()
                .name("postgres")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> containerInfo)
                .build();
        registrations.put("postgres", registration);

        Map<String, ServiceInfoSummary> result = endpoint().devServices();

        assertThat(result).hasSize(1);
        ServiceInfoSummary summary = result.get("postgres");
        assertThat(summary.description()).isNull();
    }

    @Test
    void devServiceReturnsServiceInfoWhenServiceExists() {
        ContainerInfo containerInfo = createContainerInfo("container-1", "postgres:18", "running");
        DevServiceRegistration registration = DevServiceRegistration.builder()
                .name("postgres")
                .description("PostgreSQL Database")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> containerInfo)
                .build();
        registrations.put("postgres", registration);

        ServiceInfo result = endpoint().devService("postgres");

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("postgres");
        assertThat(result.description()).isEqualTo("PostgreSQL Database");
        assertThat(result.origin()).isEqualTo(DevServiceRegistration.Origin.OWNED);
        assertThat(result.containerInfo()).isNotNull();
        assertThat(result.containerInfo().id()).isEqualTo("container-1");
        assertThat(result.containerInfo().imageName()).isEqualTo("postgres:18");
        assertThat(result.containerInfo().status()).isEqualTo("running");
        assertThat(result.containerInfo().names()).containsExactly("postgres-container");
        assertThat(result.containerInfo().labels()).containsEntry("app", "test");
        assertThat(result.containerInfo().exposedPorts()).hasSize(1);
    }

    @Test
    void devServiceReturnsNullWhenServiceNotFound() {
        ContainerInfo containerInfo = createContainerInfo("container-1", "postgres:18", "running");
        DevServiceRegistration registration = DevServiceRegistration.builder()
                .name("postgres")
                .description("PostgreSQL Database")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> containerInfo)
                .build();
        registrations.put("postgres", registration);

        assertThat(endpoint().devService("docling")).isNull();
    }

    @Test
    void devServiceReturnsNullWhenRegistrationsEmpty() {
        assertThat(endpoint().devService("postgres")).isNull();
    }

    private ContainerInfo createContainerInfo(String id, String imageName, String status) {
        List<ContainerInfo.ContainerPort> ports = List.of(
                new ContainerInfo.ContainerPort("0.0.0.0", 5432, 5432, "tcp")
        );
        return new ContainerInfo(
                id,
                imageName,
                List.of("postgres-container"),
                ports,
                Map.of("app", "test"),
                status
        );
    }

}
