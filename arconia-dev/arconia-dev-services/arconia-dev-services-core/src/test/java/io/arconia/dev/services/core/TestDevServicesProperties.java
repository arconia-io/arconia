package io.arconia.dev.services.core;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.arconia.dev.services.api.config.DevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Configuration properties for a dev service under test. The {@code with*} methods return a
 * modified copy, so the shared constants are never changed by a test.
 */
public class TestDevServicesProperties implements DevServicesProperties {

    public static final TestDevServicesProperties DEFAULT = new TestDevServicesProperties();

    public static final TestDevServicesProperties FRAMEWORK = new TestDevServicesProperties().withReuseStrategy(ReuseStrategy.FRAMEWORK);

    public static final TestDevServicesProperties TESTCONTAINERS = new TestDevServicesProperties().withReuseStrategy(ReuseStrategy.TESTCONTAINERS);

    public static final TestDevServicesProperties WITH_ENVIRONMENT = new TestDevServicesProperties()
            .withEnvironment(Map.of("KEY", "value"))
            .withNetworkAliases(List.of("db"));

    private boolean enabled = true;

    private String imageName = "test-image:latest";

    private Map<String, String> environment = Map.of();

    private List<String> networkAliases = List.of();

    private int port = 0;

    private List<ResourceMapping> resources = List.of();

    private ReuseStrategy reuseStrategy = ReuseStrategy.NONE;

    private Duration startupTimeout = Duration.ofSeconds(30);

    private List<VolumeMapping> volumes = List.of();

    public TestDevServicesProperties withEnabled(boolean enabled) {
        TestDevServicesProperties copy = copy();
        copy.enabled = enabled;
        return copy;
    }

    public TestDevServicesProperties withImageName(String imageName) {
        TestDevServicesProperties copy = copy();
        copy.imageName = imageName;
        return copy;
    }

    public TestDevServicesProperties withEnvironment(Map<String, String> environment) {
        TestDevServicesProperties copy = copy();
        copy.environment = environment;
        return copy;
    }

    public TestDevServicesProperties withNetworkAliases(List<String> networkAliases) {
        TestDevServicesProperties copy = copy();
        copy.networkAliases = networkAliases;
        return copy;
    }

    public TestDevServicesProperties withPort(int port) {
        TestDevServicesProperties copy = copy();
        copy.port = port;
        return copy;
    }

    public TestDevServicesProperties withResources(List<ResourceMapping> resources) {
        TestDevServicesProperties copy = copy();
        copy.resources = resources;
        return copy;
    }

    public TestDevServicesProperties withReuseStrategy(ReuseStrategy reuseStrategy) {
        TestDevServicesProperties copy = copy();
        copy.reuseStrategy = reuseStrategy;
        return copy;
    }

    public TestDevServicesProperties withStartupTimeout(Duration startupTimeout) {
        TestDevServicesProperties copy = copy();
        copy.startupTimeout = startupTimeout;
        return copy;
    }

    public TestDevServicesProperties withVolumes(List<VolumeMapping> volumes) {
        TestDevServicesProperties copy = copy();
        copy.volumes = volumes;
        return copy;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getImageName() {
        return imageName;
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
    public int getPort() {
        return port;
    }

    @Override
    public List<ResourceMapping> getResources() {
        return resources;
    }

    @Override
    public ReuseStrategy getReuseStrategy() {
        return reuseStrategy;
    }

    @Override
    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    @Override
    public List<VolumeMapping> getVolumes() {
        return volumes;
    }

    /**
     * A new instance with the same values, to be modified by a {@code with*} method.
     */
    protected TestDevServicesProperties copy() {
        TestDevServicesProperties copy = new TestDevServicesProperties();
        copyInto(copy);
        return copy;
    }

    /**
     * Copy the common values into the given instance.
     */
    protected void copyInto(TestDevServicesProperties target) {
        target.enabled = enabled;
        target.imageName = imageName;
        target.environment = environment;
        target.networkAliases = networkAliases;
        target.port = port;
        target.resources = resources;
        target.reuseStrategy = reuseStrategy;
        target.startupTimeout = startupTimeout;
        target.volumes = volumes;
    }

}
