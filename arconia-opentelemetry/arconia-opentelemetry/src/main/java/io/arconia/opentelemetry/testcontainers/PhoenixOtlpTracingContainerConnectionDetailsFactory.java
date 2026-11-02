package io.arconia.opentelemetry.testcontainers;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.phoenix.PhoenixContainer;

/**
 * Factory for creating {@link OtlpTracingConnectionDetails} for Phoenix containers.
 */
class PhoenixOtlpTracingContainerConnectionDetailsFactory
        extends ContainerConnectionDetailsFactory<PhoenixContainer, OtlpTracingConnectionDetails> {

    PhoenixOtlpTracingContainerConnectionDetailsFactory() {
        super(ANY_CONNECTION_NAME);
    }

    @Override
    protected OtlpTracingConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<PhoenixContainer> source) {
        return new PhoenixOtlpTracingContainerConnectionDetails(source);
    }

    private static final class PhoenixOtlpTracingContainerConnectionDetails extends ContainerConnectionDetails<PhoenixContainer> implements OtlpTracingConnectionDetails {

        private PhoenixOtlpTracingContainerConnectionDetails(ContainerConnectionSource<PhoenixContainer> source) {
            super(source);
        }

        @Override
        public String getTracesUrl(Protocol protocol) {
            return secure(switch (protocol) {
                case HTTP_PROTOBUF ->
                        "http://%s:%d%s".formatted(getContainer().getHost(), getContainer().getHttpPort(), TRACES_PATH);
                case GRPC ->
                        "http://%s:%d".formatted(getContainer().getHost(), getContainer().getGrpcPort());
            });
        }

        @Override
        public @Nullable SslBundle getSslBundle() {
            return super.getSslBundle();
        }

        private String secure(String url) {
            return getSslBundle() != null ? url.replaceFirst("^http://", "https://") : url;
        }

    }

}
