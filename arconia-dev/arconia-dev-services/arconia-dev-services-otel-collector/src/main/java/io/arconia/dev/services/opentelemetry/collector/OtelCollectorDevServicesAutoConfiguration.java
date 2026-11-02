package io.arconia.dev.services.opentelemetry.collector;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.ssl.SslBundleFiles;
import io.arconia.dev.services.opentelemetry.collector.OtelCollectorDevServicesAutoConfiguration.OtelCollectorDevServicesRegistrar;
import io.arconia.opentelemetry.autoconfigure.ConditionalOnOpenTelemetry;
import io.arconia.opentelemetry.autoconfigure.exporter.otlp.OtlpConnectionDetails;

/**
 * Auto-configuration for OpenTelemetry Collector Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("otel-collector")
@ConditionalOnOpenTelemetry
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

    static class OtelCollectorDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(OtelCollectorDevServicesProperties.CONFIG_PREFIX, OtelCollectorDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("otel-collector")
                    .description("OpenTelemetry Collector Dev Service")
                    .category(DevServiceCategories.OPENTELEMETRY)
                    .properties(properties)
                    .container(ArconiaOtelCollectorContainer.class, () -> new ArconiaOtelCollectorContainer(properties))
                    .port(ArconiaOtelCollectorContainer.OTLP_HTTP_PORT)
                    .additionalPort(ArconiaOtelCollectorContainer.OTLP_GRPC_PORT, properties.getOtlpGrpcPort())
                    .ssl(OtelCollectorDevServicesAutoConfiguration::configureSsl)
                    .serviceConnectionName(ArconiaOtelCollectorContainer.COMPATIBLE_IMAGE_NAME)
                    .discovery(OtlpConnectionDetails.class, OtelCollectorDiscoveredConnectionDetails::new)
                    .link("OTLP/gRPC", ArconiaOtelCollectorContainer.OTLP_GRPC_PORT)
                    .link("OTLP/HTTP", ArconiaOtelCollectorContainer.OTLP_HTTP_PORT));
        }

    }

}
