package io.arconia.dev.services.docling;

import org.junit.jupiter.api.Test;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DoclingDevServicesProperties}.
 */
class DoclingDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<DoclingDevServicesProperties> {

    @Override
    protected DoclingDevServicesProperties createProperties() {
        return new DoclingDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaDoclingServeContainer.COMPATIBLE_IMAGE_NAME)
                .shared(true)
                .build();
    }

    @Test
    void shouldCreateInstanceWithServiceSpecificDefaultValues() {
        DoclingDevServicesProperties properties = createProperties();

        assertThat(properties.isEnableUi()).isTrue();
        assertThat(properties.getApiKey()).isNull();
    }

    @Test
    void shouldUpdateServiceSpecificValues() {
        DoclingDevServicesProperties properties = createProperties();

        properties.setEnableUi(false);
        properties.setApiKey("caput-draconis");

        assertThat(properties.isEnableUi()).isFalse();
        assertThat(properties.getApiKey()).isEqualTo("caput-draconis");
    }

}
