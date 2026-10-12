package io.arconia.dev.services.rabbitmq;

import org.springframework.boot.amqp.autoconfigure.RabbitConnectionDetails;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.ssl.SslBundleFiles;
import io.arconia.dev.services.rabbitmq.RabbitMqDevServicesAutoConfiguration.RabbitMqDevServicesRegistrar;

/**
 * Auto-configuration for RabbitMQ Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("rabbitmq")
@EnableConfigurationProperties(RabbitMqDevServicesProperties.class)
@Import(RabbitMqDevServicesRegistrar.class)
public final class RabbitMqDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "rabbitmq";

    static final int AMQP_PORT = 5672;

    static final int AMQPS_PORT = 5671;

    static final int HTTP_PORT = 15672;

    /**
     * Where the SSL files are copied in the container.
     */
    static final String SSL_DIRECTORY = "/etc/rabbitmq/ssl/";

    /**
     * The configuration file enabling the AMQPS listener, read next to the defaults of the image.
     */
    static final String SSL_CONFIGURATION_FILE = "/etc/rabbitmq/conf.d/20-arconia-ssl.conf";

    static RabbitMQContainer createContainer(RabbitMqDevServicesProperties properties) {
        var container = new RabbitMQContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        container.withAdminUser(properties.getUsername());
        container.withAdminPassword(properties.getPassword());
        return container;
    }

    /**
     * Configure RabbitMQ to serve AMQPS with the certificate and key of the bundle, next to the
     * AMQP listener of the image. The image reads its configuration from files, so the listener is
     * declared in one, as the Testcontainers environment variables are no longer honored by it.
     */
    static void configureSsl(RabbitMQContainer container, SslBundle bundle) {
        container.withCopyToContainer(SslBundleFiles.certificateChain(bundle), SSL_DIRECTORY + "server.crt");
        container.withCopyToContainer(SslBundleFiles.privateKey(bundle), SSL_DIRECTORY + "server.key");
        container.withCopyToContainer(SslBundleFiles.certificateAuthorities(bundle), SSL_DIRECTORY + "ca.crt");
        container.withCopyToContainer(Transferable.of("""
                listeners.ssl.default = %d
                ssl_options.certfile = %sserver.crt
                ssl_options.keyfile = %sserver.key
                ssl_options.cacertfile = %sca.crt
                ssl_options.verify = verify_none
                ssl_options.fail_if_no_peer_cert = false
                """.formatted(AMQPS_PORT, SSL_DIRECTORY, SSL_DIRECTORY, SSL_DIRECTORY)), SSL_CONFIGURATION_FILE);
    }

    static class RabbitMqDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(RabbitMqDevServicesProperties.CONFIG_PREFIX, RabbitMqDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("rabbitmq")
                    .description("RabbitMQ Dev Service")
                    .properties(properties)
                    .container(RabbitMQContainer.class, () -> createContainer(properties))
                    .port(properties.getSsl().isEnabled() ? AMQPS_PORT : AMQP_PORT)
                    .additionalPort(HTTP_PORT, properties.getManagementConsolePort())
                    .ssl(RabbitMqDevServicesAutoConfiguration::configureSsl)
                    .discovery(RabbitConnectionDetails.class,
                            container -> new RabbitMqDiscoveredConnectionDetails(container, properties))
                    .link("RabbitMQ Management Console", HTTP_PORT));
        }

    }

}
