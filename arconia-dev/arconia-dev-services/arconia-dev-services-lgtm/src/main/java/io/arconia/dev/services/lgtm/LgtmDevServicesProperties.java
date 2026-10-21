package io.arconia.dev.services.lgtm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.dev.services.api.config.DevServicesProperties;
import io.arconia.dev.services.api.config.ResourceMapping;
import io.arconia.dev.services.api.config.ReuseStrategy;
import io.arconia.dev.services.api.config.VolumeMapping;

/**
 * Properties for the Grafana LGTM Dev Services.
 */
@ConfigurationProperties(prefix = LgtmDevServicesProperties.CONFIG_PREFIX)
public class LgtmDevServicesProperties implements DevServicesProperties {

    public static final String CONFIG_PREFIX = "arconia.dev.services.lgtm";

    /**
     * Whether the dev service is enabled.
     */
    private boolean enabled = true;

    /**
     * Full name of the container image used in the dev service.
     */
    private String imageName = "grafana/otel-lgtm:0.36.0";

    /**
     * Environment variables to set in the service.
     */
    private Map<String,String> environment = new HashMap<>();

    /**
     * Additional hostnames the dev service container is reachable by on the shared network,
     * next to the name of the dev service. Only applicable when the shared network is enabled.
     */
    private List<String> networkAliases = new ArrayList<>();

    /**
     * Fixed port for exposing the Grafana port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int port = 0;

    /**
     * Resources from the classpath or host filesystem to copy into the container.
     * They can be files or directories that will be copied to the specified
     * destination path inside the container at startup and are immutable (read-only).
     */
    private List<ResourceMapping> resources = new ArrayList<>();

    /**
     * Strategy for reusing a running container for the dev service across applications or restarts.
     * Only applicable in dev mode.
     */
    private ReuseStrategy reuseStrategy = ReuseStrategy.FRAMEWORK;

    /**
     * Maximum waiting time for the service to start, counted from the container start.
     */
    private Duration startupTimeout = Duration.ofMinutes(2);

    /**
     * Whether the dev service is skipped in test mode, where the application rarely needs a
     * telemetry backend. Set it to false for tests that verify the telemetry the application exports.
     */
    private boolean skipInTests = true;

    /**
     * Files or directories to mount from the host filesystem into the container.
     * They are mounted at the specified destination path inside the container
     * at startup and are mutable (read-write). Changes in either the host
     * or the container will be immediately reflected in the other.
     */
    private List<VolumeMapping> volumes = new ArrayList<>();

    /**
     * Fixed port for exposing the OTLP gRPC port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int otlpGrpcPort = 0;

    /**
     * Fixed port for exposing the OTLP HTTP port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int otlpHttpPort = 0;

    /**
     * Fixed port for exposing the Loki port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int lokiPort = 0;

    /**
     * Fixed port for exposing the Tempo port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int tempoPort = 0;

    /**
     * Fixed port for exposing the Prometheus port to the host.
     * When it's 0 (default), a random available port is assigned dynamically.
     */
    private int prometheusPort = 0;

    /**
     * Grafana dashboards provisioned in the dev service.
     */
    private Dashboards dashboards = new Dashboards();

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    @Override
    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment;
    }

    @Override
    public List<String> getNetworkAliases() {
        return networkAliases;
    }

    public void setNetworkAliases(List<String> networkAliases) {
        this.networkAliases = networkAliases;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public List<ResourceMapping> getResources() {
        return resources;
    }

    public void setResources(List<ResourceMapping> resources) {
        this.resources = resources;
    }

    @Override
    public ReuseStrategy getReuseStrategy() {
        return reuseStrategy;
    }

    public void setReuseStrategy(ReuseStrategy reuseStrategy) {
        this.reuseStrategy = reuseStrategy;
    }

    @Override
    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    @Override
    public boolean isSkipInTests() {
        return skipInTests;
    }

    public void setSkipInTests(boolean skipInTests) {
        this.skipInTests = skipInTests;
    }

    public List<VolumeMapping> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<VolumeMapping> volumes) {
        this.volumes = volumes;
    }

    public int getOtlpGrpcPort() {
        return otlpGrpcPort;
    }

    public void setOtlpGrpcPort(int otlpGrpcPort) {
        this.otlpGrpcPort = otlpGrpcPort;
    }

    public int getOtlpHttpPort() {
        return otlpHttpPort;
    }

    public void setOtlpHttpPort(int otlpHttpPort) {
        this.otlpHttpPort = otlpHttpPort;
    }

    public int getLokiPort() {
        return lokiPort;
    }

    public void setLokiPort(int lokiPort) {
        this.lokiPort = lokiPort;
    }

    public int getTempoPort() {
        return tempoPort;
    }

    public void setTempoPort(int tempoPort) {
        this.tempoPort = tempoPort;
    }

    public int getPrometheusPort() {
        return prometheusPort;
    }

    public void setPrometheusPort(int prometheusPort) {
        this.prometheusPort = prometheusPort;
    }

    public Dashboards getDashboards() {
        return dashboards;
    }

    public void setDashboards(Dashboards dashboards) {
        this.dashboards = dashboards;
    }

    /**
     * Grafana dashboards provisioned in the dev service.
     */
    public static class Dashboards {

        /**
         * Whether the dev service provisions Grafana dashboards. When disabled,
         * Grafana keeps the dashboards bundled with the container image.
         */
        private boolean enabled = true;

        /**
         * File name of the provisioned dashboard to open as the Grafana home page,
         * for example, "orders.json". By default, Grafana opens its own home page.
         */
        @Nullable
        private String home;

        /**
         * Dashboards shipped with the dev service.
         */
        private BuiltIn builtIn = new BuiltIn();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Nullable
        public String getHome() {
            return home;
        }

        public void setHome(@Nullable String home) {
            this.home = home;
        }

        public BuiltIn getBuiltIn() {
            return builtIn;
        }

        public void setBuiltIn(BuiltIn builtIn) {
            this.builtIn = builtIn;
        }

        /**
         * Dashboards shipped with the dev service.
         */
        public static class BuiltIn {

            /**
             * Whether the dashboards shipped with the dev service are provisioned,
             * next to the ones contributed by the classpath.
             */
            private boolean enabled = true;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

        }

    }

}
