package io.arconia.dev.services.elasticsearch;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchClientAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.lifecycle.TestcontainersLifecycleApplicationContextInitializer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import io.arconia.boot.bootstrap.BootstrapMode;
import io.arconia.dev.services.api.registration.DevServiceRegistration;
import io.arconia.dev.services.tests.DevServiceContainers;
import io.arconia.dev.services.tests.DisabledIfDockerUnavailable;
import io.arconia.dev.services.tests.SslTestCertificates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Elasticsearch Dev Service, with and without SSL: the application
 * connects to it through Spring Boot's service connection, over HTTP by default, over HTTPS with
 * the certificate Elasticsearch generates or the one configured through the properties.
 */
@DisabledIfDockerUnavailable
class ElasticsearchDevServicesIT {

    private static final String STARTUP_TIMEOUT = "arconia.dev.services.elasticsearch.startup-timeout=180s";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ElasticsearchDevServicesAutoConfiguration.class,
                    ServiceConnectionAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class,
                    ElasticsearchClientAutoConfiguration.class))
            .withInitializer(new TestcontainersLifecycleApplicationContextInitializer())
            .withPropertyValues(STARTUP_TIMEOUT);

    @BeforeEach
    @AfterEach
    void resetBootstrapMode() {
        BootstrapMode.clear();
    }

    @Test
    void applicationConnectsOverHttpByDefault() {
        contextRunner.run(context -> {
            ElasticsearchConnectionDetails connectionDetails = context.getBean(ElasticsearchConnectionDetails.class);
            assertThat(connectionDetails.getSslBundle()).isNull();
            assertThat(connectionDetails.getNodes()).singleElement().satisfies(node -> assertThat(node.protocol()).isEqualTo(Protocol.HTTP));
            assertClusterIsReachable(context.getBean(ElasticsearchClient.class));
        });
    }

    @Test
    void applicationConnectsOverSslWithTheCertificateElasticsearchGenerates() {
        contextRunner
                .withPropertyValues("arconia.dev.services.elasticsearch.ssl.enabled=true")
                .run(context -> {
                    ElasticsearchConnectionDetails connectionDetails = context.getBean(ElasticsearchConnectionDetails.class);
                    assertThat(connectionDetails.getSslBundle()).isNotNull();
                    assertThat(connectionDetails.getNodes()).singleElement()
                            .satisfies(node -> assertThat(node.protocol()).isEqualTo(Protocol.HTTPS));
                    assertClusterIsReachable(context.getBean(ElasticsearchClient.class));
                    // No bundle of its own: the certificate is the one of the container.
                    assertThat(context.getBean(DevServiceRegistration.class).getSslBundleName()).isNull();
                });
    }

    @Test
    void applicationConnectsOverSslWithTheConfiguredCertificate() {
        contextRunner
                .withPropertyValues(SslTestCertificates.pemProperties("elasticsearch"))
                .run(context -> {
                    ElasticsearchConnectionDetails connectionDetails = context.getBean(ElasticsearchConnectionDetails.class);
                    assertThat(connectionDetails.getNodes()).singleElement()
                            .satisfies(node -> assertThat(node.protocol()).isEqualTo(Protocol.HTTPS));
                    assertClusterIsReachable(context.getBean(ElasticsearchClient.class));
                    assertThat(context.getBean(DevServiceRegistration.class)
                            .getSslBundleName()).isEqualTo("dev-services-elasticsearch");
                });
    }

    @Test
    void applicationConnectsToDiscoveredContainerWithTheCertificateItGenerated() {
        var properties = new ElasticsearchDevServicesProperties();
        properties.getSsl().setEnabled(true);
        // Constructing a dev service container detects and caches the bootstrap mode. Clear it, or
        // the application under test never sees the dev mode it asks for.
        ElasticsearchContainer peer = DevServiceContainers.discoverable(
                ElasticsearchDevServicesAutoConfiguration.createContainer(properties), properties, "elasticsearch", "another-application");
        BootstrapMode.clear();
        try (peer) {
            peer.start();

            contextRunner
                    .withSystemProperties("arconia.bootstrap.mode=dev")
                    .withPropertyValues("arconia.dev.services.elasticsearch.reuse-strategy=framework",
                            "arconia.dev.services.elasticsearch.ssl.enabled=true")
                    .run(context -> {
                        assertThat(context).doesNotHaveBean(ElasticsearchContainer.class);
                        ElasticsearchConnectionDetails connectionDetails = context.getBean(ElasticsearchConnectionDetails.class);
                        assertThat(connectionDetails.getSslBundle()).isNotNull();
                        assertThat(connectionDetails.getNodes()).singleElement()
                                .satisfies(node -> {
                                    assertThat(node.protocol()).isEqualTo(Protocol.HTTPS);
                                    assertThat(node.port()).isEqualTo(peer.getMappedPort(ElasticsearchDevServicesAutoConfiguration.ELASTICSEARCH_DEFAULT_PORT));
                        });
                        assertClusterIsReachable(context.getBean(ElasticsearchClient.class));
                    });
        }
    }

    /**
     * Elasticsearch reports itself started before its security index is ready, which the
     * first authenticated requests answer with a 503, so the check is retried for a while.
     */
    private static void assertClusterIsReachable(ElasticsearchClient client) {
        Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofSeconds(1))
                .ignoreExceptions()
                .untilAsserted(() -> assertThat(client.cluster().health().numberOfNodes())
                        .isEqualTo(1));
    }

}
