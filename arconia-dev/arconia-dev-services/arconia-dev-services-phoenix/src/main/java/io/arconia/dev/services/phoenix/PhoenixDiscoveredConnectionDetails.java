package io.arconia.dev.services.phoenix;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslBundle;

import io.arconia.dev.services.core.registration.DiscoveredContainer;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * {@link OtlpTracingConnectionDetails} for connecting to a Phoenix dev service
 * running in a container discovered from another application. Phoenix supports
 * OpenTelemetry traces only.
 */
final class PhoenixDiscoveredConnectionDetails implements OtlpTracingConnectionDetails {

    private final String httpUrl;

    private final String grpcUrl;

    @Nullable
    private final SslBundle sslBundle;

    PhoenixDiscoveredConnectionDetails(DiscoveredContainer container) {
        this.sslBundle = container.sslBundle();
        String scheme = (sslBundle != null) ? "https" : "http";
        this.httpUrl = "%s://%s:%d".formatted(scheme, container.host(), container.mappedPort(PhoenixContainer.HTTP_PORT));
        this.grpcUrl = "%s://%s:%d".formatted(scheme, container.host(), container.mappedPort(PhoenixContainer.GRPC_PORT));
    }

    @Override
    public String getTracesUrl(Protocol protocol) {
        return switch (protocol) {
            case HTTP_PROTOBUF -> httpUrl + TRACES_PATH;
            case GRPC -> grpcUrl;
        };
    }

    @Override
    @Nullable
    public SslBundle getSslBundle() {
        return sslBundle;
    }

}
