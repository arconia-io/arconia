package io.arconia.docling.testcontainers;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.docling.autoconfigure.DoclingAutoConfiguration;
import io.arconia.docling.autoconfigure.DoclingServeConnectionDetails;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DoclingServeContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
class DoclingServeContainerConnectionDetailsFactoryTests extends DoclingTestcontainers {

    @Autowired
    DoclingServeConnectionDetails connectionDetails;

    @Test
    void shouldProvideConnectionDetailsForHttpProtobuf() {
        URI url = connectionDetails.getUrl();
        assertThat(url.toString()).isEqualTo("http://localhost:" + doclingContainer.getPort());
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DoclingAutoConfiguration.class, RestClientAutoConfiguration.class})
    static class TestConfiguration {}

}
