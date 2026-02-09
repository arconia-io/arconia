package io.arconia.dev.services.valkey;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

/**
 * Unit tests for {@link ValkeyDevServicesProperties}.
 */
class ValkeyDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<ValkeyDevServicesProperties> {

    @Override
    protected ValkeyDevServicesProperties createProperties() {
        return new ValkeyDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName(ArconiaValkeyContainer.COMPATIBLE_IMAGE_NAME)
                .build();
    }

}
