package io.arconia.docling.aot;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;

import io.arconia.docling.client.DoclingClient;
import io.arconia.docling.client.convert.request.ConvertDocumentRequest;
import io.arconia.docling.client.convert.request.options.ConvertDocumentOptions;
import io.arconia.docling.client.convert.request.options.InputFormat;
import io.arconia.docling.client.convert.response.ConvertDocumentResponse;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * Unit tests for {@link DoclingRuntimeHints}.
 */
class DoclingRuntimeHintsTests {

    @Test
    void hintsAreRegistered() {
        RuntimeHints runtimeHints = new RuntimeHints();
        DoclingRuntimeHints ollamaRuntimeHints = new DoclingRuntimeHints();
        ollamaRuntimeHints.registerHints(runtimeHints, null);

        Set<TypeReference> registeredTypes = new HashSet<>();
        runtimeHints.reflection().typeHints().forEach(typeHint -> registeredTypes.add(typeHint.getType()));

        assertThat(registeredTypes.contains(TypeReference.of(ConvertDocumentRequest.class))).isTrue();
        assertThat(registeredTypes.contains(TypeReference.of(ConvertDocumentResponse.class))).isTrue();
        assertThat(registeredTypes.contains(TypeReference.of(ConvertDocumentOptions.class))).isTrue();

        assertThat(registeredTypes.contains(TypeReference.of(InputFormat.class))).isTrue();

        assertThat(registeredTypes.contains(TypeReference.of(DoclingClient.class))).isFalse();
    }

}
