package io.arconia.dev.services.floci;

import io.arconia.dev.services.tests.BaseDevServicesPropertiesTests;

/**
 * Unit tests for {@link FlociDevServicesProperties}.
 */
class FlociDevServicesPropertiesTests extends BaseDevServicesPropertiesTests<FlociDevServicesProperties> {

    @Override
    protected FlociDevServicesProperties createProperties() {
        return new FlociDevServicesProperties();
    }

    @Override
    protected DefaultValues getExpectedDefaults() {
        return DefaultValues.builder()
                .imageName("floci/floci")
                .build();
    }

}
