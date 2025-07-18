package io.arconia.opentelemetry.autoconfigure.exporter;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.sdk.common.export.MemoryMode;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Compression;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;

/**
 * Configuration properties for OpenTelemetry exporters.
 */
@ConfigurationProperties(prefix = OpenTelemetryExporterProperties.CONFIG_PREFIX)
public class OpenTelemetryExporterProperties {

    public static final String CONFIG_PREFIX = "arconia.otel.exporter";

    /**
     * The type of OpenTelemetry exporter to use.
     */
    private ExporterType type = ExporterType.OTLP;

    /**
     * Common options for the OTLP exporters.
     */
    private final Otlp otlp = new Otlp();

    /**
     * Whether to reuse objects to reduce allocation or work with immutable data structures.
     */
    private MemoryMode memoryMode = MemoryMode.REUSABLE_DATA;

    public ExporterType getType() {
        return type;
    }

    public void setType(ExporterType type) {
        this.type = type;
    }

    public Otlp getOtlp() {
        return otlp;
    }

    public MemoryMode getMemoryMode() {
        return memoryMode;
    }

    public void setMemoryMode(MemoryMode memoryMode) {
        this.memoryMode = memoryMode;
    }

    /**
     * Configuration properties for exporting OpenTelemetry telemetry data using OTLP.
     */
    public static class Otlp {

        /**
         * The endpoint to which telemetry data will be sent.
         */
        @Nullable
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

        @Nullable
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

}
