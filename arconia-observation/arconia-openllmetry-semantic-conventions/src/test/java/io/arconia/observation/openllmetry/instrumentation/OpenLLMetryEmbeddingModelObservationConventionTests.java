package io.arconia.observation.openllmetry.instrumentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.observation.conventions.AiObservationAttributes;
import org.springframework.ai.observation.conventions.AiProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLLMetryEmbeddingModelObservationConvention}.
 */
class OpenLLMetryEmbeddingModelObservationConventionTests {

    private final OpenLLMetryEmbeddingModelObservationConvention convention =
            new OpenLLMetryEmbeddingModelObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(OpenLLMetryEmbeddingModelObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameWhenModelIsDefined() {
        EmbeddingModelObservationContext context = createContext("mistral");

        assertThat(convention.getContextualName(context)).isEqualTo("embedding mistral");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        EmbeddingModelObservationContext context = createContext("mistral");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "task"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "embeddings"),
                KeyValue.of(AiObservationAttributes.REQUEST_MODEL.value(), "mistral")
        );
    }

    @Test
    void shouldHaveLowCardinalityKeyValuesWithResponse() {
        EmbeddingModelObservationContext context = createContext("mistral");
        setResponse(context, "mistral-42");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, "task"),
                KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM, "spring_ai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "embeddings"),
                KeyValue.of(AiObservationAttributes.REQUEST_MODEL.value(), "mistral"),
                KeyValue.of(AiObservationAttributes.RESPONSE_MODEL.value(), "mistral-42")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithDimensions() {
        EmbeddingOptions options = EmbeddingOptions.builder()
                .model("mistral")
                .dimensions(1536)
                .build();
        EmbeddingRequest request = new EmbeddingRequest(List.of("Hello world"), options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_EMBEDDINGS_DIMENSION_COUNT.getKey(), "1536")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithUsage() {
        EmbeddingModelObservationContext context = createContext("mistral");
        setResponse(context, "mistral-42");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);

        assertThat(keyValues).contains(
                KeyValue.of(AiObservationAttributes.USAGE_INPUT_TOKENS.value(), "100"),
                KeyValue.of(AiObservationAttributes.USAGE_TOTAL_TOKENS.value(), "100")
        );
    }

    // Helpers

    private EmbeddingModelObservationContext createContext(String model) {
        EmbeddingOptions options = EmbeddingOptions.builder().model(model).build();
        EmbeddingRequest request = new EmbeddingRequest(List.of("Hello world"), options);
        return EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();
    }

    private void setResponse(EmbeddingModelObservationContext context, String model) {
        EmbeddingResponseMetadata metadata = new EmbeddingResponseMetadata(model, new TestUsage());
        EmbeddingResponse response = new EmbeddingResponse(List.of(), metadata);
        context.setResponse(response);
    }

    static class TestUsage implements Usage {

        @Override
        public Integer getPromptTokens() { return 100; }

        @Override
        public Integer getCompletionTokens() { return 0; }

        @Override
        public Integer getTotalTokens() { return 100; }

        @Override
        public Map<String, Integer> getNativeUsage() {
            Map<String, Integer> usage = new HashMap<>();
            usage.put("promptTokens", 100);
            return usage;
        }

    }

}
