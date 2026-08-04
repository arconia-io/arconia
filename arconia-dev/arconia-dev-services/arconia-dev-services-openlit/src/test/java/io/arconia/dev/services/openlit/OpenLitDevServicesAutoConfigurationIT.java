package io.arconia.dev.services.openlit;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceLink;
import io.arconia.dev.services.api.registration.DevServiceLinkProvider;
import io.arconia.dev.services.core.container.DevServiceLabels;
import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationIT;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.Protocol;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.otlp.OtlpLoggingConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.otlp.OtlpMetricsConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.otlp.OtlpTracingConnectionDetails;
import io.arconia.testcontainers.openlit.OpenLitContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link OpenLitDevServicesAutoConfiguration}.
 */
@EnabledIfDockerAvailable
class OpenLitDevServicesAutoConfigurationIT extends BaseDevServicesAutoConfigurationIT {

    private final ApplicationContextRunner contextRunner = defaultContextRunner(OpenLitDevServicesAutoConfiguration.class);

    @Override
    protected ApplicationContextRunner getContextRunner() {
        return contextRunner;
    }

    @Override
    protected Class<?> getAutoConfigurationClass() {
        return OpenLitDevServicesAutoConfiguration.class;
    }

    @Override
    protected Class<? extends GenericContainer<?>> getContainerClass() {
        return OpenLitContainer.class;
    }

    @Override
    protected String getServiceName() {
        return "openlit";
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
    protected boolean supportsSharedContainerDiscoveryProbing() {
        // OpenLit is a composed container that provisions a ClickHouse backend internally, so the
        // multi-stack selection probes (oldest/paused/own) would spin up several full OpenLit +
        // ClickHouse stacks. That generic registry selection logic is already covered by the other
        // dev services; the single-container discovery test below still runs.
        return false;
    }

    @Override
    protected GenericContainer<?> createSharedContainer(String ownerId) {
        OpenLitDevServicesProperties properties = new OpenLitDevServicesProperties();
        return withSharedLabels(new OpenLitContainer(DockerImageName.parse(properties.getImageName())), ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> sharedContainer) {
        assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class);
        assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);

        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).endsWith(
                ":%d%s".formatted(sharedContainer.getMappedPort(OpenLitContainer.OTLP_HTTP_PORT),
                        OtlpTracingConnectionDetails.TRACES_PATH));
        assertThat(connectionDetails.getTracesUrl(Protocol.GRPC)).endsWith(
                ":" + sharedContainer.getMappedPort(OpenLitContainer.OTLP_GRPC_PORT));
    }

    @Test
    void autoConfigurationNotActivatedWhenOpenTelemetryDisabled() {
        getContextRunner()
                .withPropertyValues("arconia.otel.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OpenLitContainer.class));
    }

    @Test
    void containerAvailableInDevMode() {
        getContextRunner()
                .withSystemProperties("arconia.bootstrap.mode=dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(getContainerClass());
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getDockerImageName()).contains(ArconiaOpenLitContainer.COMPATIBLE_IMAGE_NAME);
                    assertThat(container.isShouldBeReused()).isFalse();
                    assertThat(container.getLabels())
                            .containsEntry(DevServiceLabels.NAME, "openlit")
                            .containsEntry(DevServiceLabels.SHARED, "true")
                            .containsEntry(DevServiceLabels.OWNER, DevServiceLabels.ownerId());

                    assertThatHasSingletonScope(context);
                });
    }

    @Test
    void devServiceLinksExposeOpenLitUi() {
        getContextRunner().run(context -> {
            var container = context.getBean(getContainerClass());
            container.start();
            List<DevServiceLink> links = ((DevServiceLinkProvider) container).devServiceLinks();
            assertThat(links).singleElement().satisfies(link -> {
                assertThat(link.id()).isEqualTo("openlit");
                assertThat(link.label()).isEqualTo("OpenLit UI");
                assertThat(link.url()).startsWith("http://");
            });
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
