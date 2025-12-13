package io.arconia.docling.autoconfigure;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Docling client.
 */
@ConfigurationProperties(prefix = "arconia.docling")
public class DoclingProperties {

    /**
     * Base URL for the Docling Serve API.
     */
    private URI baseUrl = URI.create("http://localhost:5001");

    /**
     * Timeout to establish a connection to the Docling Serve API.
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * Timeout for receiving a response from the Docling Serve API.
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

}
