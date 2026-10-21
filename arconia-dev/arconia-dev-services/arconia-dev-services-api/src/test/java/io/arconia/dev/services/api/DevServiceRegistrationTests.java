package io.arconia.dev.services.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.arconia.dev.services.api.registration.ContainerInfo;
import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceRegistration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DevServiceRegistration}.
 */
class DevServiceRegistrationTests {

    private static final DevServiceRegistration.Origin MANAGED = DevServiceRegistration.Origin.MANAGED;

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRegistrations")
    void whenAComponentIsMissingThenThrow(String component, ThrowingCallable creation) {
        assertThatThrownBy(creation)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(component + " cannot be");
    }

    static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                Arguments.of("name", (ThrowingCallable) () -> validRegistration().name("").build()),
                Arguments.of("description", (ThrowingCallable) () -> validRegistration().description("").build()),
                Arguments.of("category", (ThrowingCallable) () -> validRegistration().category("").build()),
                Arguments.of("origin", (ThrowingCallable) () -> validRegistration().origin(null).build()),
                Arguments.of("containerInfo", (ThrowingCallable) () -> validRegistration().containerInfo(null).build()),
                Arguments.of("links", (ThrowingCallable) () -> validRegistration().links(null).build()));
    }

    @Test
    void whenAllFieldsAreValidThenCreate() {
        var expectedContainerInfo = createContainerInfo();

        var registration = DevServiceRegistration.builder()
                .name("test-service")
                .description("A test service")
                .origin(MANAGED)
                .containerInfo(() -> expectedContainerInfo)
                .build();

        assertThat(registration.getName()).isEqualTo("test-service");
        assertThat(registration.getDescription()).isEqualTo("A test service");
        assertThat(registration.getOrigin()).isEqualTo(MANAGED);
        assertThat(registration.getContainerInfo().get()).isEqualTo(expectedContainerInfo);
        // No category, no links, and no SSL bundle by default.
        assertThat(registration.getCategory()).isNull();
        assertThat(registration.getLinks()).isEmpty();
        assertThat(registration.getSslBundleName()).isNull();
    }

    @Test
    void whenCategoryProvidedThenExposed() {
        var registration = validRegistration().category("jdbc").build();

        assertThat(registration.getCategory()).isEqualTo("jdbc");
    }

    @Test
    void whenLinksProvidedThenDefensivelyCopiedAndImmutable() {
        var links = new ArrayList<>(List.of(new DevServiceLink("Grafana", "http://localhost:3000")));

        var registration = DevServiceRegistration.builder()
                .name("test-service")
                .description("A test service")
                .origin(MANAGED)
                .containerInfo(DevServiceRegistrationTests::createContainerInfo)
                .links(links)
                .build();

        // Mutating the original list must not affect the registration
        links.add(new DevServiceLink("OTLP/HTTP", "http://localhost:4318"));

        assertThat(registration.getLinks()).hasSize(1);
        assertThatThrownBy(() -> registration.getLinks().add(new DevServiceLink("OTLP/HTTP", "http://localhost:4318")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static DevServiceRegistration.Builder validRegistration() {
        return DevServiceRegistration.builder()
                .name("test-service")
                .description("A test service")
                .origin(MANAGED)
                .containerInfo(DevServiceRegistrationTests::createContainerInfo);
    }

    private static ContainerInfo createContainerInfo() {
        return ContainerInfo.builder()
                .id("container123")
                .imageName("docling")
                .names(List.of("docling-container"))
                .ports(List.of(new ContainerInfo.ContainerPort("127.0.0.1", 8080, 8080, "tcp")))
                .labels(Map.of("env", "dev"))
                .state("running")
                .status("running")
                .build();
    }

}
