package io.arconia.opentelemetry.autoconfigure.sdk.exporter.otlp;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for exporting OpenTelemetry telemetry data using OTLP.
 */
public class OtlpExporterConfig {

    /**
     * The endpoint to which telemetry data will be sent.
     */
    private URI endpoint;

    /**
     * The maximum waiting time for the exporter to send each telemetry batch.
     */
    private Duration timeout = Duration.ofSeconds(10);

    /**
     * The maximum waiting time for the exporter to establish a connection to the endpoint.
     */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /**
     * Transport protocol to use for OTLP requests.
     */
    private Protocol protocol = Protocol.HTTP_PROTOBUF;

    /**
     * Compression type to use for OTLP requests.
     */
    private Compression compression = Compression.GZIP;

    /**
     * Additional headers to include in each request to the endpoint.
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * Whether to generate metrics for the exporter.
     */
    private boolean metrics = false;

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

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

    public boolean isMetrics() {
        return metrics;
    }

    public void setMetrics(boolean metrics) {
        this.metrics = metrics;
    }

}
