package io.arconia.dev.services.lgtm;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.DevServiceLabels;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link LgtmDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class LgtmDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(LgtmDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return LgtmDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return LgtmStackContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "lgtm";
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
        LgtmDevServicesProperties properties = new LgtmDevServicesProperties();
        return withSharedLabels(new LgtmStackContainer(properties.getImageName()), ownerId);
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
                .run(context -> assertThat(context).doesNotHaveBean(LgtmStackContainer.class));
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaLgtmStackContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.getEnv()).contains("GF_USERS_DEFAULT_THEME=system");
                    assertThat(container.getNetworkAliases()).hasSize(1);
                    assertThat(container.isShouldBeReused()).isFalse();
                    assertThat(container.getBinds()).isEmpty();
                    assertThat(container.getLabels())
                            .containsEntry(DevServiceLabels.NAME, "lgtm")
                            .containsEntry(DevServiceLabels.SHARED, "true")
                            .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void devServiceLinksExposeGrafanaAndOtlpUrls() {
        getContextRunner().run(context -> {
            var container = context.getBean(getContainerClass());
            container.start();
            List<DevServiceLink> links = ((DevServiceLinkProvider) container).devServiceLinks();
            assertThat(links).extracting(DevServiceLink::id)
                    .containsExactly("grafana", "otlp-http", "otlp-grpc");
            assertThat(links).allSatisfy(link -> assertThat(link.url()).startsWith("http://"));
            container.stop();
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
