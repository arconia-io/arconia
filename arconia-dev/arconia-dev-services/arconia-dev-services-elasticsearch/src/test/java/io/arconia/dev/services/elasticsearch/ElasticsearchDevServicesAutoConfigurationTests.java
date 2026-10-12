package io.arconia.dev.services.elasticsearch;

import org.junit.jupiter.api.Test;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.testcontainers.beans.TestcontainerBeanDefinition;
import org.springframework.boot.testcontainers.service.connection.PemTrustStore;
import org.springframework.boot.testcontainers.service.connection.Ssl;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.MergedAnnotations;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import io.arconia.dev.services.tests.BaseDevServicesAutoConfigurationTests;
import io.arconia.dev.services.tests.DevServiceContainers;
import io.arconia.dev.services.tests.SslTestCertificates;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticsearchDevServicesAutoConfiguration}.
 */
class ElasticsearchDevServicesAutoConfigurationTests extends BaseDevServicesAutoConfigurationTests {

    ElasticsearchDevServicesAutoConfigurationTests() {
        super(ElasticsearchDevServicesAutoConfiguration.class, "elasticsearch", ElasticsearchContainer.class, ElasticsearchConnectionDetails.class);
    }

    @Override
    protected GenericContainer<?> createDiscoverableContainer(String ownerId) {
        var properties = new ElasticsearchDevServicesProperties();
        return asDiscoverableContainer(ElasticsearchDevServicesAutoConfiguration.createContainer(properties), properties, ownerId);
    }

    @Override
    protected void assertDiscoveredConnectionDetails(AssertableApplicationContext context, GenericContainer<?> discoveredContainer) {
        ElasticsearchConnectionDetails connectionDetails = context.getBean(ElasticsearchConnectionDetails.class);
        assertThat(connectionDetails.getNodes()).singleElement().satisfies(node -> {
            assertThat(node.hostname()).isEqualTo(discoveredContainer.getHost());
            assertThat(node.port()).isEqualTo(discoveredContainer.getMappedPort(ElasticsearchDevServicesAutoConfiguration.ELASTICSEARCH_DEFAULT_PORT));
        });
        assertThat(connectionDetails.getUsername()).isEqualTo("elastic");
        assertThat(connectionDetails.getPassword())
                .isEqualTo(discoveredContainer.getEnvMap().get("ELASTIC_PASSWORD"));
    }

    @Test
    void httpSslDisabledByDefault() {
        getContextRunner().run(context -> {
            var container = context.getBean(getContainerClass());
            assertThat(container.getEnvMap()).containsEntry(ElasticsearchDevServicesAutoConfiguration.HTTP_SSL_ENABLED, "false");
            assertThat(containerAnnotations(context).isPresent(Ssl.class)).isFalse();
        });
    }

    @Test
    void sslWithGeneratedCertificateApplied() {
        getContextRunner()
                .withPropertyValues("arconia.dev.services.%s.ssl.enabled=true".formatted(getServiceName()))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getEnvMap()).doesNotContainKey(ElasticsearchDevServicesAutoConfiguration.HTTP_SSL_ENABLED);
                    assertThat(containerAnnotations(context).isPresent(Ssl.class)).isTrue();
                    assertThat(containerAnnotations(context).isPresent(PemTrustStore.class)).isFalse();
                });
    }

    @Test
    void sslWithConfiguredCertificateApplied() {
        getContextRunner()
                .withPropertyValues(SslTestCertificates.pemProperties(getServiceName()))
                .run(context -> {
                    var container = context.getBean(getContainerClass());
                    assertThat(container.getEnvMap())
                            .containsEntry(ElasticsearchDevServicesAutoConfiguration.HTTP_SSL_ENABLED, "true")
                            .containsEntry("xpack.security.http.ssl.certificate", ElasticsearchDevServicesAutoConfiguration.SSL_DIRECTORY + "server.crt")
                            .containsEntry("xpack.security.http.ssl.key", ElasticsearchDevServicesAutoConfiguration.SSL_DIRECTORY + "server.key");
                    assertThat(DevServiceContainers.copiedFiles(container)).containsValues(
                            ElasticsearchDevServicesAutoConfiguration.SSL_DIRECTORY + "server.crt",
                            ElasticsearchDevServicesAutoConfiguration.SSL_DIRECTORY + "server.key");
                    assertThat(containerAnnotations(context).isPresent(PemTrustStore.class)).isTrue();
                });
    }

    private MergedAnnotations containerAnnotations(ConfigurableApplicationContext context) {
        String beanName = context.getBeanFactory().getBeanNamesForType(getContainerClass())[0];
        return ((TestcontainerBeanDefinition) context.getBeanFactory().getBeanDefinition(beanName)).getAnnotations();
    }

}
