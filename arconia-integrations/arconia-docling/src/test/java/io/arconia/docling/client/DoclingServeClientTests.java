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

import ai.docling.api.serve.DoclingServeApi;
import ai.docling.api.serve.convert.request.ConvertDocumentRequest;
import ai.docling.api.serve.convert.request.options.ConvertDocumentOptions;
import ai.docling.api.serve.convert.request.options.TableFormerMode;
import ai.docling.api.serve.convert.request.source.FileSource;
import ai.docling.api.serve.convert.request.source.HttpSource;
import ai.docling.api.serve.convert.response.ConvertDocumentResponse;
import ai.docling.api.serve.health.HealthCheckResponse;
import ai.docling.testcontainers.serve.DoclingServeContainer;
import ai.docling.testcontainers.serve.config.DoclingServeContainerConfig;

import io.arconia.docling.Images;
import io.arconia.docling.actuate.DoclingServeHealthIndicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
class DoclingServeClientTests {

    @Container
    private static final GenericContainer<?> doclingContainer = new DoclingServeContainer(DoclingServeContainerConfig.builder()
            .image(Images.DOCLING)
            .build());

    private static DoclingServeApi doclingServeApi;

    @BeforeAll
    static void setUp() {
        doclingServeApi = createDoclingServeApi();
    }

    @Test
    void shouldSuccessfullyCallHealthEndpoint() {
        HealthCheckResponse response = doclingServeApi.health();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ok");
    }

    @Test
    void shouldIntegrateWithDoclingHealthIndicator() {
        var healthIndicator = new DoclingServeHealthIndicator(doclingServeApi);
        var health = healthIndicator.health();

        assertThat(health).isNotNull();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void shouldConvertHttpSourceSuccessfully() {
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .source(HttpSource.builder()
                        .url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                        .build())
                .build();

        ConvertDocumentResponse response = doclingServeApi.convertSource(request);

        assertThat(response).isNotNull();

        assertThat(response.getStatus()).isNotEmpty();
        assertThat(response.getDocument()).isNotNull();
        assertThat(response.getDocument().getFilename()).isNotEmpty();

        if (response.getProcessingTime() != null) {
            assertThat(response.getProcessingTime()).isPositive();
        }

        assertThat(response.getDocument().getMarkdownContent()).isNotEmpty();
    }

    @Test
    void shouldConvertFileSourceSuccessfully() throws IOException {
        var fileResource = new ClassPathResource("story.pdf").getContentAsByteArray();
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .source(FileSource.builder()
                        .filename("story.pdf")
                        .base64String(Base64.getEncoder().encodeToString(fileResource))
                        .build())
                .build();

        ConvertDocumentResponse response = doclingServeApi.convertSource(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotEmpty();
        assertThat(response.getDocument()).isNotNull();
        assertThat(response.getDocument().getFilename()).isEqualTo("story.pdf");

        if (response.getProcessingTime() != null) {
            assertThat(response.getProcessingTime()).isPositive();
        }

        assertThat(response.getDocument().getMarkdownContent()).isNotEmpty();
    }

    @Test
    void shouldHandleConversionWithDifferentDocumentOptions() {
        ConvertDocumentOptions options = ConvertDocumentOptions.builder()
                .doOcr(true)
                .includeImages(true)
                .tableMode(TableFormerMode.FAST)
                .build();

        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .source(HttpSource.builder()
                        .url(URI.create("https://docs.arconia.io/arconia-cli/latest/development/dev/"))
                        .build())
                .options(options)
                .build();

        ConvertDocumentResponse response = doclingServeApi.convertSource(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isNotEmpty();
        assertThat(response.getDocument()).isNotNull();
    }

    @Test
    void shouldHandleErrorsGracefullyWhenConvertingInvalidSources() {
        ConvertDocumentRequest request = ConvertDocumentRequest.builder()
                .source(HttpSource.builder()
                        .url(URI.create("https://invalid-domain-that-should-not-exist-12345.com/document.pdf"))
                        .build())
                .options(ConvertDocumentOptions.builder().build())
                .build();

        // Docling returns 404 Not Found when a file doesn't exist or cannot be accessed
        assertThatThrownBy(() -> doclingServeApi.convertSource(request))
                .isInstanceOf(HttpClientErrorException.NotFound.class)
                .hasMessageContaining("404 Not Found");
    }

    private static DoclingServeApi createDoclingServeApi() {
        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + doclingContainer.getMappedPort(5001))
                .requestFactory(ClientHttpRequestFactoryBuilder.jdk()
                        .withHttpClientCustomizer(builder -> builder.version(HttpClient.Version.HTTP_1_1))
                        .build())
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();

        return factory.createClient(DoclingServeClient.class);
    }

}
