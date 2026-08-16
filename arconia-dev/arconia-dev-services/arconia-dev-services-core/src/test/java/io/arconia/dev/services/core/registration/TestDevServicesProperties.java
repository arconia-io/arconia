package io.arconia.dev.services.core.registration;

import io.arconia.dev.services.api.config.SharedDevServicesProperties;

/**
 * Configuration properties for a dev service under test, as every registration
 * must declare the properties the registry reads configured values from.
 */
record TestDevServicesProperties(boolean shared) implements SharedDevServicesProperties {

    static final TestDevServicesProperties DEFAULT = new TestDevServicesProperties(false);

    static final TestDevServicesProperties SHARED = new TestDevServicesProperties(true);

    @Override
    public String getImageName() {
        return "test-image:latest";
    }

    @Override
    public boolean isShared() {
        return shared;
    }

}
