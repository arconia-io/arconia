package io.arconia.dev.services.phoenix;

import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * {@link OtlpTracingConnectionDetails} for connecting to a shared Phoenix dev service
 * running in a container discovered from another application. Phoenix supports
 * OpenTelemetry traces only.
 */
final class PhoenixDiscoveredConnectionDetails implements OtlpTracingConnectionDetails {

    private final String httpUrl;

    private final String grpcUrl;

    PhoenixDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.httpUrl = "http://%s:%d".formatted(container.host(), container.mappedPort(PhoenixContainer.HTTP_PORT));
        this.grpcUrl = "http://%s:%d".formatted(container.host(), container.mappedPort(PhoenixContainer.GRPC_PORT));
    }

    @Override
    public String getTracesUrl(Protocol protocol) {
        return switch (protocol) {
            case HTTP_PROTOBUF -> httpUrl + TRACES_PATH;
            case GRPC -> grpcUrl;
        };
    }

}
