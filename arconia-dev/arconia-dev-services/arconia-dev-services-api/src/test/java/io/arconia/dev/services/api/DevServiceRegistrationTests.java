package io.arconia.dev.services.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DevServiceRegistration}.
 */
class DevServiceRegistrationTests {

    @Test
    void whenNameIsNullThenThrow() {
        assertThatThrownBy(() -> DevServiceRegistration.builder()
                .description("A test service")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(this::createContainerInfo)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenNameIsEmptyThenThrow() {
        assertThatThrownBy(() -> DevServiceRegistration.builder()
                .name("")
                .description("A test service")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(this::createContainerInfo)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenOriginIsNullThenThrow() {
        assertThatThrownBy(() -> DevServiceRegistration.builder()
                .name("test-service")
                .description("A test service")
                .containerInfo(this::createContainerInfo)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("origin cannot be null");
    }

    @Test
    void whenContainerInfoIsNullThenThrow() {
        assertThatThrownBy(() -> DevServiceRegistration.builder()
                .name("test-service")
                .description("A test service")
                .origin(DevServiceRegistration.Origin.OWNED)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("containerInfo cannot be null");
    }

    @Test
    void whenAllFieldsAreValidThenCreate() {
        var expectedContainerInfo = createContainerInfo();

        var registration = DevServiceRegistration.builder()
                .name("test-service")
                .description("A test service")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> expectedContainerInfo)
                .build();

        assertThat(registration.name()).isEqualTo("test-service");
        assertThat(registration.description()).isEqualTo("A test service");
        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.OWNED);
        assertThat(registration.containerInfo()).isNotNull();
        assertThat(registration.containerInfo().get()).isEqualTo(expectedContainerInfo);
    }

    @Test
    void whenDescriptionIsNullThenCreate() {
        var expectedContainerInfo = createContainerInfo();

        var registration = DevServiceRegistration.builder()
                .name("test-service")
                .origin(DevServiceRegistration.Origin.DISCOVERED)
                .containerInfo(() -> expectedContainerInfo)
                .build();

        assertThat(registration.name()).isEqualTo("test-service");
        assertThat(registration.description()).isNull();
        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
        assertThat(registration.containerInfo()).isNotNull();
        assertThat(registration.containerInfo().get()).isEqualTo(expectedContainerInfo);
    }

    @Test
    void whenLinksProvidedThenDefensivelyCopiedAndImmutable() {
        var expectedContainerInfo = createContainerInfo();
        var links = new ArrayList<>(List.of(
                new DevServiceLink("grafana", "Grafana", "http://localhost:3000")));

        var registration = DevServiceRegistration.builder()
                .name("test-service")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> expectedContainerInfo)
                .links(links)
                .build();

        // Mutating the original list must not affect the registration
        links.add(new DevServiceLink("otlp", "OTLP/HTTP", "http://localhost:4318"));

        assertThat(registration.links()).hasSize(1);
        assertThatThrownBy(() -> registration.links().add(
                new DevServiceLink("otlp", "OTLP/HTTP", "http://localhost:4318")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void whenLinksNotProvidedThenEmpty() {
        var expectedContainerInfo = createContainerInfo();

        var registration = DevServiceRegistration.builder()
                .name("test-service")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> expectedContainerInfo)
                .build();

        assertThat(registration.links()).isEmpty();
    }

    private ContainerInfo createContainerInfo() {
        return ContainerInfo.builder()
                .id("container123")
                .imageName("docling")
                .names(List.of("docling-container"))
                .exposedPorts(List.of(new ContainerInfo.ContainerPort("127.0.0.1", 8080, 8080, "tcp")))
                .labels(Map.of("env", "dev"))
                .status("running")
                .build();
    }

}
