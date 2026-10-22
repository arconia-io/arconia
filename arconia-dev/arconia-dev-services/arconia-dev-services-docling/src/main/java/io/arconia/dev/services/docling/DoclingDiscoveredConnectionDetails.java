package io.arconia.dev.services.docling;

import java.net.URI;

import org.jspecify.annotations.Nullable;

import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.docling.autoconfigure.DoclingServeConnectionDetails;

/**
 * {@link DoclingServeConnectionDetails} for connecting to a Docling dev service
 * running in a container discovered from another application.
 */
final class DoclingDiscoveredConnectionDetails implements DoclingServeConnectionDetails {

    static final String API_KEY_ENVIRONMENT_VARIABLE = "DOCLING_SERVE_API_KEY";

    private final URI baseUrl;

    @Nullable
    private final String apiKey;

    DoclingDiscoveredConnectionDetails(DiscoveredContainer container, DoclingDevServicesProperties properties) {
        this.baseUrl = URI.create("http://%s:%d".formatted(container.host(), container.mappedPort(DEFAULT_PORT)));
        // As for a container the application starts itself, an API key set through the
        // environment variables takes precedence over the api-key property.
        this.apiKey = properties.getEnvironment().getOrDefault(API_KEY_ENVIRONMENT_VARIABLE, properties.getApiKey());
    }

    @Override
    public URI getBaseUrl() {
        return baseUrl;
    }

    @Override
    @Nullable
    public String getApiKey() {
        return apiKey;
    }

}
