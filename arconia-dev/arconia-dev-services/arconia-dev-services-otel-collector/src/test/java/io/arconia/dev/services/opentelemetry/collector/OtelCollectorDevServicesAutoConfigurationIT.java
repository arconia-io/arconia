package io.arconia.dev.services.opentelemetry.collector;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.api.registration.DevServiceLabels;
import io.arconia.dev.services.api.registration.DevServiceLinkDefinition;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OtelCollectorDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OtelCollectorDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(OtelCollectorDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return OtelCollectorDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return ArconiaOtelCollectorContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "otel-collector";
    }

    @Override
    protected Class<?> getConnectionDetailsClass() {
        return OtlpTracingConnectionDetails.class;
    }

    @Override
    protected boolean supportsSharing() {
        return true;
    }

    @Override
    protected GenericContainer<?> createSharedContainer(String ownerId) {
        OtelCollectorDevServicesProperties properties = new OtelCollectorDevServicesProperties();
        GenericContainer<?> container = new GenericContainer<>(properties.getImageName())
                .withExposedPorts(OtlpConnectionDetails.DEFAULT_GRPC_PORT, OtlpConnectionDetails.DEFAULT_HTTP_PORT);
        return withSharedLabels(container, ownerId);
    }

    @Override
    protected List<DevServiceLinkDefinition> sharedContainerLinkDefinitions() {
        return new ArconiaOtelCollectorContainer(new OtelCollectorDevServicesProperties()).devServiceLinkDefinitions();
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> sharedContainer) {
        assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class);
        assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);

        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).endsWith(
                ":%d%s".formatted(sharedContainer.getMappedPort(OtlpConnectionDetails.DEFAULT_HTTP_PORT),
                        OtlpTracingConnectionDetails.TRACES_PATH));
        assertThat(connectionDetails.getTracesUrl(Protocol.GRPC)).endsWith(
                ":" + sharedContainer.getMappedPort(OtlpConnectionDetails.DEFAULT_GRPC_PORT));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ArconiaOtelCollectorContainer.class));
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaOtelCollectorContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).isEmpty();
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isFalse();
                    assertThat(container.getLabels())
                            .containsEntry(DevServiceLabels.NAME, "otel-collector")
                            .containsEntry(DevServiceLabels.SHARED, "true")
                            .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void containerConfigurationApplied() {
        String[] properties = ArrayUtils.addAll(commonConfigurationProperties());

        getContextRunner()
                .withPropertyValues(properties)
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    container.start();
                    assertThatConfigurationIsApplied(container);
                    container.stop();
                });
    }

}
