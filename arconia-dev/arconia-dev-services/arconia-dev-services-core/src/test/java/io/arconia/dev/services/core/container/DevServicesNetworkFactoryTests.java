package io.arconia.dev.services.core.container;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;

import io.arconia.boot.bootstrap.BootstrapMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServicesNetworkFactory}.
 */
class DevServicesNetworkFactoryTests {

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        System.clearProperty(BootstrapMode.PROPERTY_KEY);
        BootstrapMode.clear();
    }

    @Test
    void whenNameIsNullThenReturnsSharedNetwork() {
        enableDevMode();
        assertThat(DevServicesNetworkFactory.resolve(null)).isSameAs(Network.SHARED);
    }

    @Test
    void whenNameIsBlankThenReturnsSharedNetwork() {
        enableDevMode();
        assertThat(DevServicesNetworkFactory.resolve("   ")).isSameAs(Network.SHARED);
    }

    @Test
    void whenNotInDevModeThenReturnsSharedNetworkEvenWithName() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "test");
        BootstrapMode.clear();
        assertThat(DevServicesNetworkFactory.resolve("arconia")).isSameAs(Network.SHARED);
    }

    @Test
    void whenReservedNameInDevModeThenReturnsSharedNetwork() {
        enableDevMode();
        assertThat(DevServicesNetworkFactory.resolve("bridge")).isSameAs(Network.SHARED);
        assertThat(DevServicesNetworkFactory.resolve("HOST")).isSameAs(Network.SHARED);
        assertThat(DevServicesNetworkFactory.resolve("container:other")).isSameAs(Network.SHARED);
    }

    private void enableDevMode() {
        System.setProperty(BootstrapMode.PROPERTY_KEY, "dev");
        BootstrapMode.clear();
    }

}
