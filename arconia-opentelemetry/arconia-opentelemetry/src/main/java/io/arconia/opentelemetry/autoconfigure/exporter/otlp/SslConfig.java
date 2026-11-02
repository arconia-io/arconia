package io.arconia.opentelemetry.autoconfigure.exporter.otlp;

import org.jspecify.annotations.Nullable;

/**
 * Configuration for securing the connection to the OTLP endpoint with TLS.
 */
public class SslConfig {

    /**
     * Name of the SSL bundle to use for the connection to the OTLP endpoint.
     */
    @Nullable
    private String bundle;

    @Nullable
    public String getBundle() {
        return bundle;
    }

    public void setBundle(@Nullable String bundle) {
        this.bundle = bundle;
    }

}
