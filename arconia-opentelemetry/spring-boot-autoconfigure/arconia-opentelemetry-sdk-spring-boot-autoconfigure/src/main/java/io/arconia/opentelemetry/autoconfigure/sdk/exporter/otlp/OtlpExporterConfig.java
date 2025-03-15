package io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Configuration properties for exporting OpenTelemetry telemetry data using OTLP.
 */
public class OtlpExporterConfig {

    /**
     * The endpoint to which telemetry data will be sent.
     */
    @Nullable
    private URI endpoint;

    /**
     * The maximum waiting time for the exporter to send each telemetry batch.
     */
    @Nullable
    private Duration timeout;

    /**
     * The maximum waiting time for the exporter to establish a connection to the endpoint.
     */
    @Nullable
    private Duration connectTimeout;

    /**
     * Transport protocol to use for OTLP requests.
     */
    @Nullable
    private Protocol protocol;

    /**
     * Compression type to use for OTLP requests.
     */
    @Nullable
    private Compression compression;

    /**
     * Additional headers to include in each request to the endpoint.
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * Whether to generate metrics for the exporter.
     */
    @Nullable
    private Boolean metrics;

    @Nullable
    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    @Nullable
    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    @Nullable
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    @Nullable
    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    @Nullable
    public Compression getCompression() {
        return compression;
    }

    public void setCompression(Compression compression) {
        this.compression = compression;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Nullable
    public Boolean isMetrics() {
        return metrics;
    }

    public void setMetrics(Boolean metrics) {
        this.metrics = metrics;
    }

}
