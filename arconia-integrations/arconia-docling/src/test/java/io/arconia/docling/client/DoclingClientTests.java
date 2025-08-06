package io.arconia.docling.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Base64;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.arconia.docling.Images;
import io.arconia.docling.actuate.DoclingHealthIndicator;
import io.arconia.docling.autoconfigure.client.DoclingConnectionDetails;
import io.arconia.docling.client.convert.request.ConvertDocumentOptions;
import io.arconia.docling.client.convert.request.ConvertDocumentRequest;
import io.arconia.docling.client.convert.request.options.TableFormerMode;
import io.arconia.docling.client.convert.response.ConvertDocumentResponse;
import io.arconia.docling.client.health.HealthCheckResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class DoclingClientTests {

    @Container
    private static final GenericContainer<?> doclingContainer = new GenericContainer<>(Images.DOCLING)
            .withExposedPorts(DoclingConnectionDetails.DEFAULT_PORT);

    private static DoclingClient doclingClient;

    @BeforeAll
    static void setUp() {
        doclingClient = createDoclingClient();
    }

    @Test
    void shouldSuccessfullyCallHealthEndpoint() {
        HealthCheckResponse response = doclingClient.health();

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("ok");
    }

    @Test
    void shouldIntegrateWithDoclingHealthIndicator() {
        var healthIndicator = new DoclingHealthIndicator(doclingClient);
        var health = healthIndicator.health();

        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void shouldConvertHttpSourceSuccessfully() {
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .addHttpSources(URI.create("https://arconia.io/docs/arconia-cli/latest/development/dev/"))
                .build();

        ConvertDocumentResponse response = doclingClient.convertSource(request);

        assertThat(response).isNotNull();

        assertThat(response.status()).isNotEmpty();
        assertThat(response.document()).isNotNull();
        assertThat(response.document().filename()).isNotEmpty();

        if (response.processingTime() != null) {
            assertThat(response.processingTime()).isPositive();
        }

        assertThat(response.document().markdownContent()).isNotEmpty();
    }

    @Test
    void shouldConvertFileSourceSuccessfully() throws IOException {
        var fileResource = new ClassPathResource("story1.pdf").getContentAsByteArray();
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .addFileSources("story1.pdf", Base64.getEncoder().encodeToString(fileResource))
                .build();

        ConvertDocumentResponse response = doclingClient.convertSource(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isNotEmpty();
        assertThat(response.document()).isNotNull();
        assertThat(response.document().filename()).isEqualTo("story1.pdf");

        if (response.processingTime() != null) {
            assertThat(response.processingTime()).isPositive();
        }

        assertThat(response.document().markdownContent()).isNotEmpty();
    }

    @Test
    void shouldHandleConversionWithDifferentDocumentOptions() {
        ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                .doOcr(true)
                .includeImages(true)
                .tableMode(TableFormerMode.FAST)
                .build();

        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .addHttpSources(URI.create("https://arconia.io/docs/arconia-cli/latest/development/dev/"))
                .options(options)
                .build();

        ConvertDocumentResponse response = doclingClient.convertSource(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isNotEmpty();
        assertThat(response.document()).isNotNull();
    }

    @Test
    void shouldHandleErrorsGracefullyWhenConvertingInvalidSources() {
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .addHttpSources(URI.create("https://invalid-domain-that-should-not-exist-12345.com/document.pdf"))
                .options(ConvertDocumentOptions.builder().build())
                .build();

        // Docling returns 404 Not Found when a file doesn't exist or cannot be accessed
        assertThatThrownBy(() -> doclingClient.convertSource(request))
                .isInstanceOf(HttpClientErrorException.NotFound.class)
                .hasMessageContaining("404 Not Found");
    }

    private static DoclingClient createDoclingClient() {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + doclingContainer.getMappedPort(5001))
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk()
                        .withHttpClientCustomizer(builder -> builder.version(HttpClient.Version.HTTP_1_1))
                        .build())
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(DoclingClient.class);
    }

}
