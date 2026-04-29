package io.arconia.observation.openllmetry.instrumentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.observation.conventions.AiProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryEmbeddingModelObservationConvention}.
 */
class OpenLLMetryEmbeddingModelObservationConventionTests {

    private final OpenLLMetryOptions openLLMetryOptions = new OpenLLMetryOptions();
    private final OpenLLMetryEmbeddingModelObservationConvention observationConvention
            = new OpenLLMetryEmbeddingModelObservationConvention(openLLMetryOptions);

    @Test
    void shouldHaveName() {
        assertThat(observationConvention.getName())
            .isEqualTo(OpenLLMetryEmbeddingModelObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameWhenModelIsDefined() {
        EmbeddingOptions options = EmbeddingOptions.builder().model("mistral").build();
        EmbeddingRequest request = new EmbeddingRequest(List.of(), options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getContextualName(context)).isEqualTo("embedding mistral");
    }

    @Test
    void contextualNameWhenModelIsNotDefined() {
        EmbeddingRequest request = new EmbeddingRequest(List.of(), EmbeddingOptions.builder().build());
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getContextualName(context)).isEqualTo("embedding");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        EmbeddingOptions options = EmbeddingOptions.builder().model("mistral").build();
        EmbeddingRequest request = new EmbeddingRequest(List.of(), options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "task"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_OPERATION_NAME, "embeddings"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, "mistral")
        );
    }

    @Test
    void shouldHaveLowCardinalityKeyValuesWithResponse() {
        EmbeddingOptions options = EmbeddingOptions.builder().model("mistral").build();
        EmbeddingRequest request = new EmbeddingRequest(List.of(), options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();
        EmbeddingResponseMetadata responseMetadata = new EmbeddingResponseMetadata("mistral-42", new TestUsage());
        EmbeddingResponse response = new EmbeddingResponse(List.of(new Embedding(new float[]{4.2f}, 0)), responseMetadata);
        context.setResponse(response);

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "task"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_OPERATION_NAME, "embeddings"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, "mistral-42")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithRequestOnly() throws JSONException {
        List<String> instructions = List.of("Embed this text", "Embed this other text");
        EmbeddingOptions options = EmbeddingOptions.builder().model("mistral").build();
        EmbeddingRequest request = new EmbeddingRequest(instructions, options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        String entityInput = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT);
        JSONAssert.assertEquals("""
                ["Embed this text","Embed this other text"]""",
                entityInput, JSONCompareMode.STRICT);
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithResponse() throws JSONException {
        List<String> instructions = List.of("Embed this text", "And this one too");
        EmbeddingOptions options = EmbeddingOptions.builder().model("mistral").build();
        EmbeddingRequest request = new EmbeddingRequest(instructions, options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        EmbeddingResponseMetadata responseMetadata = new EmbeddingResponseMetadata("mistral-42", new TestUsage());
        List<Embedding> embeddings = List.of(
                new Embedding(new float[]{0.1f, 0.2f, 0.3f}, 0),
                new Embedding(new float[]{0.4f, 0.5f, 0.6f}, 1)
        );
        EmbeddingResponse response = new EmbeddingResponse(embeddings, responseMetadata);
        context.setResponse(response);

        KeyValues keyValues = observationConvention.getHighCardinalityKeyValues(context);

        String entityOutput = findKeyValue(keyValues, OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT);
        JSONAssert.assertEquals("""
                ["<3 dimensional vector>","<3 dimensional vector>"]""",
                entityOutput, JSONCompareMode.STRICT);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_USAGE_INPUT_TOKENS, "1000"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_USAGE_TOTAL_TOKENS, "1000")
        );
    }

    @Test
    void shouldRedactHighCardinalityKeyValuesWhenTraceContentDisabled() {
        List<String> instructions = List.of("Embed this text");
        EmbeddingOptions options = EmbeddingOptions.builder().model("mistral").build();
        EmbeddingRequest request = new EmbeddingRequest(instructions, options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();
        EmbeddingResponseMetadata responseMetadata = new EmbeddingResponseMetadata("mistral-42", new TestUsage());
        List<Embedding> embeddings = List.of(
                new Embedding(new float[]{0.1f, 0.2f, 0.3f}, 0)
        );
        EmbeddingResponse response = new EmbeddingResponse(embeddings, responseMetadata);
        context.setResponse(response);

        OpenLLMetryOptions redactingOptions = new OpenLLMetryOptions();
        redactingOptions.setTraceContent(false);
        var redactingConvention = new OpenLLMetryEmbeddingModelObservationConvention(redactingOptions);

        KeyValues keyValues = redactingConvention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER),
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER)
        );
    }

    private String findKeyValue(KeyValues keyValues, String key) {
        return keyValues.stream()
                .filter(kv -> Objects.equals(kv.getKey(), key))
                .findFirst()
                .map(KeyValue::getValue)
                .orElseThrow(() -> new AssertionError("Key not found: " + key));
    }

    static class TestUsage implements Usage {

        @Override
        public Integer getPromptTokens() {
            return 1000;
        }

        @Override
        public Integer getCompletionTokens() {
            return 0;
        }

        @Override
        public Map<String, Integer> getNativeUsage() {
            Map<String, Integer> usage = new HashMap<>();
            usage.put("promptTokens", getPromptTokens());
            usage.put("completionTokens", getCompletionTokens());
            usage.put("totalTokens", getTotalTokens());
            return usage;
        }

    }

}
