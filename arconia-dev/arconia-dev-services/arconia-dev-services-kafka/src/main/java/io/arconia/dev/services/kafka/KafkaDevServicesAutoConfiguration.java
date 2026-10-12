package io.arconia.dev.services.kafka;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.autoconfigure.DevServicesAutoConfiguration;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.ssl.SslBundleFiles;
import io.arconia.dev.services.kafka.KafkaDevServicesAutoConfiguration.KafkaDevServicesRegistrar;

/**
 * Auto-configuration for Kafka Dev Services.
 */
@AutoConfiguration(after = DevServicesAutoConfiguration.class, before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("kafka")
@EnableConfigurationProperties(KafkaDevServicesProperties.class)
@Import(KafkaDevServicesRegistrar.class)
public final class KafkaDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "apache/kafka-native";

    static final int KAFKA_PORT = 9092;

    static final String READY_REGEX = ".*Transitioning from RECOVERY to RUNNING.*";

    /**
     * Where the SSL files are copied in the container. The directory the image reserves for
     * secrets is a volume, which would hide files copied into it before the container starts.
     */
    static final String SSL_DIRECTORY = "/arconia/ssl/";

    static KafkaContainer createContainer(KafkaDevServicesProperties properties) {
        var container = new KafkaContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        // KafkaContainer waits on a wait strategy instance shared across all Kafka containers,
        // so when we customize the startup timeout, it will be applied to all of them.
        // Hence, we must provide an equivalent wait strategy of our own.
        container.waitingFor(Wait.forLogMessage(READY_REGEX, 1));
        return container;
    }

    /**
     * Configure the broker to serve the listener published to the host over SSL, with the
     * certificate and key of the bundle in PEM form. The listener keeps the name the container
     * declares it with, since it is the name the advertised listeners refer to; only its protocol
     * changes. The broker and controller listeners, internal to the container, stay in plaintext.
     */
    static void configureSsl(KafkaContainer container, SslBundle bundle) {
        container.withCopyToContainer(SslBundleFiles.certificateChainWithPrivateKey(bundle), SSL_DIRECTORY + "keystore.pem");
        container.withCopyToContainer(SslBundleFiles.certificateAuthorities(bundle), SSL_DIRECTORY + "truststore.pem");
        container
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:SSL,CONTROLLER:PLAINTEXT")
                .withEnv("KAFKA_SSL_KEYSTORE_TYPE", "PEM")
                .withEnv("KAFKA_SSL_KEYSTORE_LOCATION", SSL_DIRECTORY + "keystore.pem")
                .withEnv("KAFKA_SSL_TRUSTSTORE_TYPE", "PEM")
                .withEnv("KAFKA_SSL_TRUSTSTORE_LOCATION", SSL_DIRECTORY + "truststore.pem")
                .withEnv("KAFKA_SSL_CLIENT_AUTH", "none");
    }

    static class KafkaDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(KafkaDevServicesProperties.CONFIG_PREFIX, KafkaDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("kafka")
                    .description("Kafka Dev Service")
                    .properties(properties)
                    .container(KafkaContainer.class, () -> createContainer(properties))
                    .port(KAFKA_PORT)
                    .ssl(KafkaDevServicesAutoConfiguration::configureSsl)
                    .discovery(KafkaConnectionDetails.class, KafkaDiscoveredConnectionDetails::new));
        }

    }

}
