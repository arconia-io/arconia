package io.arconia.dev.services.core.registration;

import java.util.List;
import java.util.Map;

import io.arconia.dev.services.api.config.BaseDevServicesProperties;
import io.arconia.dev.services.api.config.ReuseStrategy;

/**
 * Configuration properties for a dev service under test, as every registration
 * must declare the properties the registry reads configured values from.
 */
record TestDevServicesProperties(ReuseStrategy reuseStrategy, Map<String, String> environment, List<String> networkAliases) implements BaseDevServicesProperties {

    TestDevServicesProperties(ReuseStrategy reuseStrategy) {
        this(reuseStrategy, Map.of(), List.of());
    }

    static final TestDevServicesProperties DEFAULT = new TestDevServicesProperties(ReuseStrategy.NONE);

    static final TestDevServicesProperties FRAMEWORK = new TestDevServicesProperties(ReuseStrategy.FRAMEWORK);

    static final TestDevServicesProperties TESTCONTAINERS = new TestDevServicesProperties(ReuseStrategy.TESTCONTAINERS);

    static final TestDevServicesProperties WITH_ENVIRONMENT = new TestDevServicesProperties(ReuseStrategy.NONE, Map.of("KEY", "value"), List.of("db"));

    @Override
    public String getImageName() {
        return "test-image:latest";
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    @Override
    public List<String> getNetworkAliases() {
        return networkAliases;
    }

    @Override
    public ReuseStrategy getReuseStrategy() {
        return reuseStrategy;
    }

}
