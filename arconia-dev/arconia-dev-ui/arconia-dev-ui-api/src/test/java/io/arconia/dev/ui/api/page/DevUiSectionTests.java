package io.arconia.dev.ui.api.page;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiSection}.
 */
class DevUiSectionTests {

    @Test
    void configurationSectionComesBeforeThePagesWithTheDefaultOrder() {
        assertThat(DevUiSection.CONFIGURATION.getId()).isEqualTo("configuration");
        assertThat(DevUiSection.CONFIGURATION.getTitle()).isEqualTo("Configuration");
        assertThat(DevUiSection.CONFIGURATION.getIcon()).isEqualTo("settings");
        assertThat(DevUiSection.CONFIGURATION.getOrder()).isNegative();
        assertThat(DevUiSection.CONFIGURATION).hasToString("Configuration");
    }

}
