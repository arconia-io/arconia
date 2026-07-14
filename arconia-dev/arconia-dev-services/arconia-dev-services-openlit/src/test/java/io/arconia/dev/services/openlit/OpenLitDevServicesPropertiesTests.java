package io.arconia.dev.services.openlit;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLitDevServicesProperties}.
 */
class OpenLitDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<OpenLitDevServicesProperties> {

    @Override
    protected OpenLitDevServicesProperties createProperties() {
        return new OpenLitDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaOpenLitContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .startupTimeout(Duration.ofMinutes(2))
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        OpenLitDevServicesProperties properties = createProperties();

        assertThat(properties.getOtlpGrpcPort()).isEqualTo(0);
        assertThat(properties.getOtlpHttpPort()).isEqualTo(0);
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        OpenLitDevServicesProperties properties = createProperties();

        properties.setOtlpGrpcPort(9001);
        properties.setOtlpHttpPort(9002);

        assertThat(properties.getOtlpGrpcPort()).isEqualTo(9001);
        assertThat(properties.getOtlpHttpPort()).isEqualTo(9002);
    }

}
