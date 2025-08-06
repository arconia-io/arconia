package io.arconia.docling.autoconfigure.client;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import io.arconia.core.support.Incubating;

/**
 * Configuration properties for the Docling client.
 */
@ConfigurationProperties(prefix = "arconia.docling")
@Incubating(since = "0.15.0")
public class DoclingClientProperties {

    /**
     * The base URL for the Docling API.
     */
    private URI url = URI.create("http://localhost:5001");

    /**
     * Timeout to establish a connection to the Docling API.
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * Timeout for receving a response from the Docling API.
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
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
