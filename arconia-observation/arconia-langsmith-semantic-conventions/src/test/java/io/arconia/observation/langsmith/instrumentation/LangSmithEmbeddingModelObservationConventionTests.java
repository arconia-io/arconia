package io.arconia.observation.langsmith.instrumentation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.micrometer.common.KeyValue;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.observation.conventions.AiProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithEmbeddingModelObservationConvention}.
 */
class LangSmithEmbeddingModelObservationConventionTests {

    private final LangSmithEmbeddingModelObservationConvention convention =
            new LangSmithEmbeddingModelObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(LangSmithEmbeddingModelObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameWhenModelIsDefined() {
        EmbeddingModelObservationContext context = createContext("text-embedding-3-small");

        assertThat(convention.getContextualName(context)).isEqualTo("embeddings text-embedding-3-small");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        EmbeddingModelObservationContext context = createContext("text-embedding-3-small");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "embedding"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "embeddings"),
                KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(), "openai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), "text-embedding-3-small")
        );
    }

    @Test
    void shouldHaveLowCardinalityKeyValuesWithResponse() {
        EmbeddingModelObservationContext context = createContext("text-embedding-3-small");
        setResponse(context, "text-embedding-3-small-v2");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_RESPONSE_MODEL.getKey(), "text-embedding-3-small-v2")
        );
    }

    // Token usage is set by LangSmithEmbeddingModelObservationHandler with proper numeric types

    private EmbeddingModelObservationContext createContext(String model) {
        EmbeddingOptions embeddingOptions = EmbeddingOptions.builder().model(model).build();
        EmbeddingRequest request = new EmbeddingRequest(List.of("Hello world"), embeddingOptions);
        return EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.OPENAI.value())
                .build();
    }

    private void setResponse(EmbeddingModelObservationContext context, String model) {
        EmbeddingResponseMetadata metadata = new EmbeddingResponseMetadata(model, new TestUsage(), Map.of());
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
