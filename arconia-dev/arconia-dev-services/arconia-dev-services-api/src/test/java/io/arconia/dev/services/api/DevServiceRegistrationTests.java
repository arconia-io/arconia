package io.arconia.dev.services.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DevServiceRegistration}.
 */
class DevServiceRegistrationTests {

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRegistrations")
    void whenAComponentIsMissingThenThrow(String component, ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(component + " cannot be null");
    }

    static Stream<Arguments> invalidRegistrations() {
        Supplier<ContainerInfo> containerInfo = DevServiceRegistrationTests::createContainerInfo;
        return Stream.of(
                Arguments.of("name", (ThrowingCallable) () -> new DevServiceRegistration("", null, DevServiceRegistration.Origin.OWNED, containerInfo, List.of())),
                Arguments.of("origin", (ThrowingCallable) () -> new DevServiceRegistration("test-service", null, null, containerInfo, List.of())),
                Arguments.of("containerInfo", (ThrowingCallable) () -> new DevServiceRegistration("test-service", null, DevServiceRegistration.Origin.OWNED, null, List.of())));
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
    void whenLinksProvidedThenDefensivelyCopiedAndImmutable() {
        var expectedContainerInfo = createContainerInfo();
        var links = new ArrayList<>(List.of(
                new DevServiceLink("Grafana", "http://localhost:3000")));

        var registration = DevServiceRegistration.builder()
                .name("test-service")
                .origin(DevServiceRegistration.Origin.OWNED)
                .containerInfo(() -> expectedContainerInfo)
                .links(links)
                .build();

        // Mutating the original list must not affect the registration
        links.add(new DevServiceLink("OTLP/HTTP", "http://localhost:4318"));

        assertThat(registration.links()).hasSize(1);
        assertThatThrownBy(() -> registration.links().add(
                new DevServiceLink("OTLP/HTTP", "http://localhost:4318")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static ContainerInfo createContainerInfo() {
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
