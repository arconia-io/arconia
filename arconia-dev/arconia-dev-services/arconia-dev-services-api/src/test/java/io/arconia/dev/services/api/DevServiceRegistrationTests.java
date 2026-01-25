package io.arconia.dev.services.api;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DevServiceRegistration}.
 */
class DevServiceRegistrationTests {

    @Test
    void whenNameIsNullThenThrow() {
        Supplier<ContainerInfo> containerInfoSupplier = this::createContainerInfo;

        assertThatThrownBy(() -> new DevServiceRegistration(null, "A test service", containerInfoSupplier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenNameIsEmptyThenThrow() {
        Supplier<ContainerInfo> containerInfoSupplier = this::createContainerInfo;

        assertThatThrownBy(() -> new DevServiceRegistration("", "A test service", containerInfoSupplier))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name cannot be null or empty");
    }

    @Test
    void whenContainerInfoIsNullThenThrow() {
        assertThatThrownBy(() -> new DevServiceRegistration("test-service", "A test service", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("containerInfo cannot be null");
    }

    @Test
    void whenAllFieldsAreValidThenCreate() {
        var expectedContainerInfo = createContainerInfo();
        Supplier<ContainerInfo> containerInfoSupplier = () -> expectedContainerInfo;

        var registration = new DevServiceRegistration("test-service", "A test service", containerInfoSupplier);

        assertThat(registration.name()).isEqualTo("test-service");
        assertThat(registration.description()).isEqualTo("A test service");
        assertThat(registration.containerInfo()).isNotNull();
        assertThat(registration.containerInfo().get()).isEqualTo(expectedContainerInfo);
    }

    @Test
    void whenDescriptionIsNullThenCreate() {
        var expectedContainerInfo = createContainerInfo();
        Supplier<ContainerInfo> containerInfoSupplier = () -> expectedContainerInfo;

        var registration = new DevServiceRegistration("test-service", null, containerInfoSupplier);

        assertThat(registration.name()).isEqualTo("test-service");
        assertThat(registration.description()).isNull();
        assertThat(registration.containerInfo()).isNotNull();
        assertThat(registration.containerInfo().get()).isEqualTo(expectedContainerInfo);
    }

    private ContainerInfo createContainerInfo() {
        return new ContainerInfo(
                "container123",
                "docling",
                List.of("docling-container"),
                List.of(new ContainerInfo.ContainerPort("127.0.0.1", 8080, 8080, "tcp")),
                Map.of("env", "dev"),
                "running"
        );
    }

}
