package io.arconia.docling.autoconfigure;

import java.net.URI;
import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Docling client.
 */
@ConfigurationProperties(prefix = "arconia.docling")
public class DoclingProperties {

    public static final String API_KEY_HEADER_NAME = "X-Api-Key";

    /**
     * Base URL for the Docling Serve API.
     */
    private URI baseUrl = URI.create("http://localhost:5001");

    /**
     * API key to be used for authenticating requests to the Docling Serve API.
     */
    @Nullable
    private String apiKey;

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

    @Nullable
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(@Nullable String apiKey) {
        this.apiKey = apiKey;
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
