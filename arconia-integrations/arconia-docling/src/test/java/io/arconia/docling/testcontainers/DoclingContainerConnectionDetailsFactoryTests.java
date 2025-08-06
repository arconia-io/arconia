package io.arconia.docling.testcontainers;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.docling.autoconfigure.client.DoclingClientAutoConfiguration;
import io.arconia.docling.autoconfigure.client.DoclingConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DoclingContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
class DoclingContainerConnectionDetailsFactoryTests extends DoclingTestcontainers {

    @Autowired
    DoclingConnectionDetails connectionDetails;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        URI url = connectionDetails.getUrl();
        assertThat(url.toString()).isEqualTo("http://localhost:" + doclingContainer.getMappedPort(DoclingConnectionDetails.DEFAULT_PORT));
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DoclingClientAutoConfiguration.class, RestClientAutoConfiguration.class})
    static class TestConfiguration {}

}
