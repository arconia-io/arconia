package io.arconia.dev.services.mongodb;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import io.arconia.dev.services.core.autoconfigure.ConditionalOnDevServicesEnabled;
import io.arconia.dev.services.core.registration.DevServiceCategories;
import io.arconia.dev.services.core.registration.DevServicesRegistrar;
import io.arconia.dev.services.core.registration.DevServicesRegistry;
import io.arconia.dev.services.core.ssl.SslBundleFiles;
import io.arconia.dev.services.mongodb.MongoDbDevServicesAutoConfiguration.MongoDbDevServicesRegistrar;

/**
 * Auto-configuration for MongoDB Dev Services.
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnDevServicesEnabled("mongodb")
@EnableConfigurationProperties(MongoDbDevServicesProperties.class)
@Import(MongoDbDevServicesRegistrar.class)
public final class MongoDbDevServicesAutoConfiguration {

    static final String COMPATIBLE_IMAGE_NAME = "mongo";

    static final int MONGODB_PORT = 27017;

    /**
     * Where the SSL files are copied in the container.
     */
    static final String SSL_DIRECTORY = "/arconia/ssl/";

    static MongoDBContainer createContainer(MongoDbDevServicesProperties properties) {
        var container = new MongoDBContainer(DockerImageName.parse(properties.getImageName()).asCompatibleSubstituteFor(COMPATIBLE_IMAGE_NAME));
        // A single-node replica set, so that transactions and change streams work as they do
        // against a production deployment. It also gives the container its own wait strategy.
        container.withReplicaSet();
        return container;
    }

    /**
     * Configure MongoDB to serve SSL, with the certificate and key of the bundle in the
     * single file it expects them in. Clients are not asked for a certificate. SSL is
     * preferred rather than required: the application connects over SSL through its
     * service connection, while the replica set is initiated from inside the container
     * over a plain connection.
     */
    static void configureSsl(MongoDBContainer container, SslBundle bundle) {
        container.withCopyToContainer(SslBundleFiles.certificateChainWithPrivateKey(bundle), SSL_DIRECTORY + "server.pem");
        container.withCopyToContainer(SslBundleFiles.certificateAuthorities(bundle), SSL_DIRECTORY + "ca.crt");
        String[] command = container.getCommandParts();
        List<String> parts = new ArrayList<>((command != null) ? List.of(command) : List.of());
        parts.addAll(List.of(
                "--tlsMode", "preferTLS",
                "--tlsCertificateKeyFile", SSL_DIRECTORY + "server.pem",
                "--tlsCAFile", SSL_DIRECTORY + "ca.crt",
                "--tlsAllowConnectionsWithoutCertificates"));
        container.withCommand(parts.toArray(new String[0]));
    }

    static class MongoDbDevServicesRegistrar extends DevServicesRegistrar {

        @Override
        protected void registerDevServices(DevServicesRegistry registry, Environment environment) {
            var properties = bindProperties(MongoDbDevServicesProperties.CONFIG_PREFIX, MongoDbDevServicesProperties.class);

            registry.registerDevService(service -> service
                    .name("mongodb")
                    .description("MongoDB Dev Service")
                    .category(DevServiceCategories.MONGODB)
                    .properties(properties)
                    .container(MongoDBContainer.class, () -> createContainer(properties))
                    .port(MONGODB_PORT)
                    .ssl(MongoDbDevServicesAutoConfiguration::configureSsl)
                    .discovery(MongoConnectionDetails.class, MongoDbDiscoveredConnectionDetails::new));
        }

    }

}
