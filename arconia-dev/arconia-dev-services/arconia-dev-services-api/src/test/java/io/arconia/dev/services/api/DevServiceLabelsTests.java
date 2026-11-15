package io.arconia.dev.services.api;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.api.registration.DevServiceLabels;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevServiceLabels}.
 */
class DevServiceLabelsTests {

    @Test
    void labelNamesAreAContractBetweenApplications() {
        assertThat(DevServiceLabels.NAME).isEqualTo("io.arconia.dev-service.name");
        assertThat(DevServiceLabels.DISCOVERABLE).isEqualTo("io.arconia.dev-service.discoverable");
        assertThat(DevServiceLabels.OWNER).isEqualTo("io.arconia.dev-service.owner");
    }

    @Test
    void ownerIdIsStableWithinTheApplication() {
        assertThat(DevServiceLabels.ownerId()).isNotBlank().isEqualTo(DevServiceLabels.ownerId());
    }

}
