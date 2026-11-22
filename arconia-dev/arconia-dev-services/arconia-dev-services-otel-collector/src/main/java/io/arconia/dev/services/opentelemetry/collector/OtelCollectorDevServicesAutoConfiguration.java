package io.arconia.dev.services.opentelemetry.collector;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.ssl.SslBundleFiles;
import io.arconia.dev.services.opentelemetry.collector.OtelCollectorDevServicesAutoConfiguration.OtelCollectorDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;
import io.arconia.opentelemetry.autoconfigure.logs.exporter.ConditionalOnOpenTelemetryLoggingExporter;
import io.arconia.opentelemetry.autoconfigure.metrics.exporter.ConditionalOnOpenTelemetryMetricsExporter;
import io.arconia.opentelemetry.autoconfigure.traces.exporter.ConditionalOnOpenTelemetryTracingExporter;

/**
 * Auto-configuration for OpenTelemetry Collector Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("otel-collector")
@Conditional(OtelCollectorDevServicesAutoConfiguration.OtlpExportEnabled.class)
@EnableConfigurationProperties(OtelCollectorDevServicesProperties.class)
@Import(OtelCollectorDevServicesRegistrar.class)
public final class OtelCollectorDevServicesAutoConfiguration {

    /**
     * Where the SSL files are copied in the container.
     */
    static final String SSL_DIRECTORY = "/arconia/ssl/";

    /**
     * The configuration file the image runs with by default.
     */
    static final String CONFIG_FILE = "/etc/otelcol-contrib/config.yaml";

    /**
     * Configure the OTLP receiver to serve both its gRPC and HTTP ports over SSL, with the certificate
     * and key of the bundle. The settings override the configuration file from the command line. The image
     * has no shell, and a command replaces its default arguments, so the configuration file is named again.
     */
    static void configureSsl(ArconiaOtelCollectorContainer container, SslBundle bundle) {
        container.withCopyToContainer(SslBundleFiles.certificateChain(bundle), SSL_DIRECTORY + "server.crt");
        container.withCopyToContainer(SslBundleFiles.privateKey(bundle), SSL_DIRECTORY + "server.key");
        List<String> command = new ArrayList<>(List.of("--config", CONFIG_FILE));
        for (String protocol : List.of("grpc", "http")) {
            command.add("--set=receivers::otlp::protocols::%s::tls::cert_file=%sserver.crt".formatted(protocol, SSL_DIRECTORY));
            command.add("--set=receivers::otlp::protocols::%s::tls::key_file=%sserver.key".formatted(protocol, SSL_DIRECTORY));
        }
        container.withCommand(command.toArray(new String[0]));
    }

    /**
     * The dev service is only started when the application exports at least one signal via OTLP,
     * since it would receive nothing otherwise, as it is the case in tests by default.
     */
    static class OtlpExportEnabled extends AnyNestedCondition {

        OtlpExportEnabled() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnOpenTelemetryLoggingExporter("otlp")
        static class LogsExportEnabled {}

        @ConditionalOnOpenTelemetryMetricsExporter("otlp")
        static class MetricsExportEnabled {}

        @ConditionalOnOpenTelemetryTracingExporter("otlp")
        static class TracesExportEnabled {}

    }

    static class OtelCollectorDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OtelCollectorDevServicesProperties.CONFIG_PREFIX, OtelCollectorDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("otel-collector")
                    .title("OpenTelemetry Collector")
                    .description("Vendor-agnostic service for receiving, processing, and exporting telemetry data.")
                    .icon("telescope")
                    .category(DevServiceCategories.OPENTELEMETRY)
                    .properties(properties)
                    .container(ArconiaOtelCollectorContainer.class, () -> new ArconiaOtelCollectorContainer(properties))
                    .port(ArconiaOtelCollectorContainer.OTLP_HTTP_PORT)
                    .additionalPort(ArconiaOtelCollectorContainer.OTLP_GRPC_PORT, properties.getOtlpGrpcPort())
                    .ssl(OtelCollectorDevServicesAutoConfiguration::configureSsl)
                    .serviceConnectionName(ArconiaOtelCollectorContainer.COMPATIBLE_IMAGE_NAME)
                    .discovery(OtlpConnectionDetails.class, OtelCollectorDiscoveredConnectionDetails::new));
        }

    }

}
