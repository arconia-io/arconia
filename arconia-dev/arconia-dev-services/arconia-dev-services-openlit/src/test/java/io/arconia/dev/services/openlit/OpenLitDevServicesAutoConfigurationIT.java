package io.arconia.dev.services.openlit;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.api.registration.DevServiceRegistration;
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
    void sharedContainerDiscoveredWhenStartedByAnotherApplication() {
        OpenLitDevServicesProperties properties = new OpenLitDevServicesProperties();
        try (OpenLitContainer sharedContainer = new OpenLitContainer(DockerImageName.parse(properties.getImageName()))
                .withLabel(DevServiceLabels.NAME, "openlit")
                .withLabel(DevServiceLabels.SHARED, "true")
                .withLabel(DevServiceLabels.OWNER, "another-application")) {
            sharedContainer.start();

            getContextRunner()
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(getContainerClass());
                        assertThat(context).hasSingleBean(OtlpTracingConnectionDetails.class);
                        assertThat(context).hasSingleBean(OtlpMetricsConnectionDetails.class);
                        assertThat(context).hasSingleBean(OtlpLoggingConnectionDetails.class);

                        OtlpTracingConnectionDetails connectionDetails = context.getBean(OtlpTracingConnectionDetails.class);
                        assertThat(connectionDetails.getTracesUrl(Protocol.HTTP_PROTOBUF)).endsWith(
                                ":%d%s".formatted(sharedContainer.getMappedPort(OpenLitContainer.OTLP_HTTP_PORT),
                                        OtlpTracingConnectionDetails.TRACES_PATH));
                        assertThat(connectionDetails.getTracesUrl(Protocol.GRPC)).endsWith(
                                ":" + sharedContainer.getMappedPort(OpenLitContainer.OTLP_GRPC_PORT));

                        assertThat(context).hasSingleBean(DevServiceRegistration.class);
                        DevServiceRegistration registration = context.getBean(DevServiceRegistration.class);
                        assertThat(registration.origin()).isEqualTo(DevServiceRegistration.Origin.DISCOVERED);
                        assertThat(registration.containerInfo().get().id()).isEqualTo(sharedContainer.getContainerId());
                    });
        }
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
