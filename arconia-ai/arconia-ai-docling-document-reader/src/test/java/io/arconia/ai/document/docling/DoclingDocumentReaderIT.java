package io.arconia.ai.document.docling;

import java.util.List;
import java.util.Map;

import ai.docling.serve.api.DoclingServeApi;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import io.arconia.docling.autoconfigure.DoclingAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DoclingDocumentReader}.
 */
@SpringJUnitConfig
class DoclingDocumentReaderIT extends DoclingTestcontainers {

    @Autowired
    DoclingServeApi doclingServeApi;

    @Test
    void whenFileThenReturnDocuments() {
        Resource file = new ClassPathResource("story.pdf");
        List<Document> documents = DoclingDocumentReader.builder()
                .files(file)
                .doclingServeApi(doclingServeApi)
                .metadata(Map.of("location", "North Pole"))
                .build()
                .get();

        assertThat(documents).isNotEmpty();
        assertThat(documents)
                .allMatch(document -> document.getMetadata().get("location")
                        .equals("North Pole"))
                .allMatch(document -> document.getMetadata().get(DocumentMetadata.BINARY_HASH.value()) != null)
                .allMatch(document -> document.getMetadata().get(DocumentMetadata.FILE_NAME.value())
                        .equals("story.pdf"))
                .allMatch(document -> document.getMetadata().get(DocumentMetadata.HEADINGS.value())
                        .equals(List.of("The Adventures of Iorek and Pingu")))
                .allMatch(document -> document.getMetadata().get(DocumentMetadata.MIME_TYPE.value())
                        .equals("application/pdf"))
                .allMatch(document -> document.getMetadata().get(DocumentMetadata.PAGE_NUMBERS.value()) instanceof List
                        && !((List<?>) document.getMetadata().get(DocumentMetadata.PAGE_NUMBERS.value())).isEmpty());

    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DoclingAutoConfiguration.class, RestClientAutoConfiguration.class})
    static class TestConfiguration {}

}
