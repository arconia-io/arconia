package io.arconia.docling.testcontainers;

import java.net.URI;

import ai.docling.serve.api.DoclingServeApi;
import ai.docling.serve.api.clear.request.ClearConvertersRequest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.HttpClientErrorException;

import io.arconia.docling.autoconfigure.DoclingAutoConfiguration;
import io.arconia.docling.autoconfigure.DoclingServeConnectionDetails;
import io.arconia.docling.client.DoclingServeClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for {@link DoclingServeContainerConnectionDetailsFactory}.
 */
@SpringJUnitConfig
class DoclingServeContainerConnectionDetailsFactoryIT extends DoclingTestcontainers {

    @Autowired
    DoclingServeConnectionDetails connectionDetails;

    @Autowired
    DoclingServeApi doclingServeApi;

    @Test
    void shouldProvideConnectionDetails() {
        assertThat(connectionDetails.getBaseUrl()).isEqualTo(URI.create("http://localhost:" + doclingContainer.getPort()));
        assertThat(connectionDetails.getApiKey()).isEqualTo(API_KEY);
    }

    @Test
    void shouldAuthenticateWithTheApiKeyOfTheContainer() {
        assertThat(doclingServeApi.clearConverters(ClearConvertersRequest.builder().build())).isNotNull();

        DoclingServeClient clientWithoutApiKey = DoclingServeClient.builder()
                .baseUrl(connectionDetails.getBaseUrl())
                .build();
        assertThatThrownBy(() -> clientWithoutApiKey.clearConverters(ClearConvertersRequest.builder().build()))
                .isInstanceOf(HttpClientErrorException.class);
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DoclingAutoConfiguration.class, RestClientAutoConfiguration.class})
    static class TestConfiguration {}

}
