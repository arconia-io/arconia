package io.arconia.openinference.observation.instrumentation.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.observation.conventions.AiProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenInferenceEmbeddingModelObservationConvention}.
 */
class OpenInferenceEmbeddingModelObservationConventionTests {

    private final OpenInferenceTracingOptions tracingOptions = new OpenInferenceTracingOptions();
    private final OpenInferenceEmbeddingModelObservationConvention observationConvention
            = new OpenInferenceEmbeddingModelObservationConvention(tracingOptions);

    @Test
    void shouldHaveName() {
        assertThat(observationConvention.getName())
            .isEqualTo(OpenInferenceEmbeddingModelObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameWhenModelIsDefined() {
        EmbeddingOptions options = EmbeddingOptionsBuilder.builder().withModel("mistral").build();
        EmbeddingRequest request = new EmbeddingRequest(List.of(), options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getContextualName(context)).isEqualTo("embedding mistral");
    }

    @Test
    void contextualNameWhenModelIsNotDefined() {
        EmbeddingRequest request = new EmbeddingRequest(List.of(), EmbeddingOptionsBuilder.builder().build());
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getContextualName(context)).isEqualTo("embedding");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        EmbeddingOptions options = EmbeddingOptionsBuilder.builder().withModel("mistral").build();
        EmbeddingRequest request = new EmbeddingRequest(List.of(), options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, "EMBEDDING"),
                KeyValue.of(SemanticConventions.LLM_PROVIDER, "spring_ai"),
                KeyValue.of(SemanticConventions.LLM_SYSTEM, "spring_ai"),
                KeyValue.of(SemanticConventions.EMBEDDING_MODEL_NAME, "mistral")
        );
    }

    @Test
    void shouldHaveLowCardinalityKeyValuesWithResponse() {
        EmbeddingOptions options = EmbeddingOptionsBuilder.builder().withModel("mistral").build();
        EmbeddingRequest request = new EmbeddingRequest(List.of(), options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();
        EmbeddingResponseMetadata responseMetadata = new EmbeddingResponseMetadata("mistral-42", new TestUsage());
        EmbeddingResponse response = new EmbeddingResponse(List.of(new Embedding(new float[]{4.2f}, 0)), responseMetadata);
        context.setResponse(response);

        assertThat(observationConvention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, "EMBEDDING"),
                KeyValue.of(SemanticConventions.LLM_PROVIDER, "spring_ai"),
                KeyValue.of(SemanticConventions.LLM_SYSTEM, "spring_ai"),
                KeyValue.of(SemanticConventions.EMBEDDING_MODEL_NAME, "mistral-42")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithRequestOnly() {
        List<String> instructions = List.of("Embed this text", "Embed this other text");
        EmbeddingOptions options = EmbeddingOptionsBuilder.builder()
                .withModel("mistral")
                .withDimensions(128)
                .build();
        EmbeddingRequest request = new EmbeddingRequest(instructions, options);
        EmbeddingModelObservationContext context = EmbeddingModelObservationContext.builder()
                .embeddingRequest(request)
                .provider(AiProvider.SPRING_AI.value())
                .build();

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.EMBEDDING_EMBEDDINGS + ".0." + SemanticConventions.EMBEDDING_TEXT, "Embed this text"),
                KeyValue.of(SemanticConventions.EMBEDDING_EMBEDDINGS + ".1." + SemanticConventions.EMBEDDING_TEXT, "Embed this other text"),
                KeyValue.of(SemanticConventions.LLM_INVOCATION_PARAMETERS, """
                        {"dimensions":128}""")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithResponse() {
        List<String> instructions = List.of("Embed this text", "And this one too");
        EmbeddingOptions options = EmbeddingOptionsBuilder.builder()
                .withModel("mistral")
                .withDimensions(3)
                .build();
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

        assertThat(observationConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(SemanticConventions.EMBEDDING_EMBEDDINGS + ".0." + SemanticConventions.EMBEDDING_TEXT, "Embed this text"),
                KeyValue.of(SemanticConventions.EMBEDDING_EMBEDDINGS + ".1." + SemanticConventions.EMBEDDING_TEXT, "And this one too"),
                KeyValue.of(SemanticConventions.EMBEDDING_EMBEDDINGS + ".0." + SemanticConventions.EMBEDDING_VECTOR, "<3 dimensional vector>"),
                KeyValue.of(SemanticConventions.EMBEDDING_EMBEDDINGS + ".1." + SemanticConventions.EMBEDDING_VECTOR, "<3 dimensional vector>"),
                KeyValue.of(SemanticConventions.LLM_TOKEN_COUNT_PROMPT, "1000"),
                KeyValue.of(SemanticConventions.LLM_TOKEN_COUNT_TOTAL, "1000"),
                KeyValue.of(SemanticConventions.LLM_INVOCATION_PARAMETERS, """
                        {"dimensions":3}""")
        );
    }

    @Test
    void shouldRedactHighCardinalityKeyValuesWhenConfigured() {
        List<String> instructions = List.of("Embed this text");
        EmbeddingOptions options = EmbeddingOptionsBuilder.builder()
                .withModel("mistral")
                .withDimensions(3)
                .build();
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

        String embeddingTextKey = SemanticConventions.EMBEDDING_EMBEDDINGS + ".0." + SemanticConventions.EMBEDDING_TEXT;
        String embeddingVectorKey = SemanticConventions.EMBEDDING_EMBEDDINGS + ".0." + SemanticConventions.EMBEDDING_VECTOR;

        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideLlmInvocationParameters,
                SemanticConventions.LLM_INVOCATION_PARAMETERS);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideInputs, embeddingTextKey);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideInputMessages, embeddingTextKey);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideInputText, embeddingTextKey);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideOutputs, embeddingVectorKey);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideOutputMessages, embeddingVectorKey);
        assertRedactionBehavior(context, OpenInferenceTracingOptions::setHideEmbeddingVectors, embeddingVectorKey);
    }

    private void assertRedactionBehavior(EmbeddingModelObservationContext context,
                                         java.util.function.BiConsumer<OpenInferenceTracingOptions, Boolean> optionSetter,
                                         String expectedKey) {
        OpenInferenceTracingOptions redactingOptions = new OpenInferenceTracingOptions();
        OpenInferenceEmbeddingModelObservationConvention redactingConvention =
                new OpenInferenceEmbeddingModelObservationConvention(redactingOptions);
        optionSetter.accept(redactingOptions, true);

        assertThat(redactingConvention.getHighCardinalityKeyValues(context)).contains(
                KeyValue.of(expectedKey, OpenInferenceTracingOptions.REDACTED_PLACEHOLDER)
        );
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
