package io.arconia.dev.services.docling;

import java.net.URI;

import org.jspecify.annotations.Nullable;

import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.docling.autoconfigure.DoclingServeConnectionDetails;

/**
 * {@link DoclingServeConnectionDetails} for connecting to a shared Docling dev service
 * running in a container discovered from another application.
 * <p>
 * The API key comes from the local configuration properties: it must match
 * the one used by the application that started the shared container, if any.
 */
final class DoclingDiscoveredConnectionDetails implements DoclingServeConnectionDetails {

    private final URI baseUrl;

    @Nullable
    private final String apiKey;

    DoclingDiscoveredConnectionDetails(DiscoveredContainer container, DoclingDevServicesProperties properties) {
        this.baseUrl = URI.create("http://%s:%d".formatted(container.host(), container.mappedPort(DEFAULT_PORT)));
        this.apiKey = properties.getApiKey();
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
