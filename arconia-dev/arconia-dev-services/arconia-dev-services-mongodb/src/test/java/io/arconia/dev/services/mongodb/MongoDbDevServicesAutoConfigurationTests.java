package io.arconia.dev.services.mongodb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ssl.PemSslBundleProperties;
import org.springframework.boot.autoconfigure.ssl.PropertiesSslBundle;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mongodb.MongoDBContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DevServiceContainers;
import io.arconia.dev.services.tests.SslTestCertificates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MongoDbDevServicesAutoConfiguration}.
 */
class MongoDbDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    MongoDbDevServicesAutoConfigurationTests() {
        super(MongoDbDevServicesAutoConfiguration.class, "mongodb", MongoDBContainer.class, MongoConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new MongoDbDevServicesProperties();
        return asDiscoverableContainer(MongoDbDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        MongoConnectionDetails connectionDetails = context.getBean(MongoConnectionDetails.class);
        assertThat(connectionDetails.getConnectionString().getConnectionString())
                .isEqualTo(((MongoDBContainer) discoveredContainer).getReplicaSetUrl());
    }

    @Test
    void sslApplied() {
        getContextRunner()
                .withPropertyValues(SslTestCertificates.pemProperties(getServiceName()))
                .run(context -> {
                    var container = (MongoDBContainer) context.getBean(getContainerClass());
                    assertThat(container.getCommandParts()).containsSequence("--tlsMode", "preferTLS")
                            .contains("--tlsAllowConnectionsWithoutCertificates");
                    assertThat(DevServiceContainers.copiedFiles(container)).containsValues(
                            MongoDbDevServicesAutoConfiguration.SSL_DIRECTORY + "server.pem",
                            MongoDbDevServicesAutoConfiguration.SSL_DIRECTORY + "ca.crt");
                });
    }

    @Test
    void containerRunsAReplicaSet() {
        var container = MongoDbDevServicesAutoConfiguration.createContainer(new MongoDbDevServicesProperties());

        assertThat(container.getCommandParts()).containsSequence("--replSet", "docker-rs");
    }

    @Test
    void containerStartsOnAnyLinuxKernel() {
        var container = MongoDbDevServicesAutoConfiguration.createContainer(new MongoDbDevServicesProperties());

        assertThat(container.getEnvMap()).containsEntry(MongoDbDevServicesAutoConfiguration.GLIBC_TUNABLES, "glibc.pthread.rseq=1");
    }

    @Test
    void sslArgumentsAreAppendedToTheReplicaSetCommand() {
        var container = MongoDbDevServicesAutoConfiguration.createContainer(new MongoDbDevServicesProperties());
        PemSslBundleProperties properties = new PemSslBundleProperties();
        properties.getKeystore().setCertificate(SslTestCertificates.CERTIFICATE);
        properties.getKeystore().setPrivateKey(SslTestCertificates.PRIVATE_KEY);

        MongoDbDevServicesAutoConfiguration.configureSsl(container, PropertiesSslBundle.get(properties));

        assertThat(container.getCommandParts()).startsWith("--replSet", "docker-rs").containsSequence("--tlsMode", "preferTLS");
    }

}
