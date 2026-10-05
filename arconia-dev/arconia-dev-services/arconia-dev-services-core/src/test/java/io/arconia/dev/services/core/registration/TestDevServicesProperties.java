package io.arconia.dev.services.core.registration;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ReuseStrategy;

/**
 * Configuration properties for a dev service under test, as every registration
 * must declare the properties the registry reads configured values from.
 */
record TestDevServicesProperties(ReuseStrategy reuseStrategy) implements BaseDevServicesProperties {

    static final TestDevServicesProperties DEFAULT = new TestDevServicesProperties(ReuseStrategy.NONE);

    static final TestDevServicesProperties FRAMEWORK = new TestDevServicesProperties(ReuseStrategy.FRAMEWORK);

    static final TestDevServicesProperties TESTCONTAINERS = new TestDevServicesProperties(ReuseStrategy.TESTCONTAINERS);

    @Override
    public String getImageName() {
        return "test-image:latest";
    }

    @Override
    public ReuseStrategy getReuseStrategy() {
        return reuseStrategy;
    }

}
