package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import org.jspecify.annotations.Nullable;

/**
 * Configuration for connecting to the OTLP endpoint through an HTTP proxy.
 */
public class ProxyConfig {

    /**
     * Host of the proxy. When not set, the proxy configured for the JVM is used, if any.
     */
    @Nullable
    private String host;

    /**
     * Port of the proxy.
     */
    @Nullable
    private Integer port;

    @Nullable
    public String getHost() {
        return host;
    }

    public void setHost(@Nullable String host) {
        this.host = host;
    }

    @Nullable
    public Integer getPort() {
        return port;
    }

    public void setPort(@Nullable Integer port) {
        this.port = port;
    }

}
