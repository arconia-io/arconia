package io.arconia.openinference.observation.instrumentation.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public final class OpenInferenceEmbeddingModelObservationConvention implements EmbeddingModelObservationConvention {

    public static final String DEFAULT_NAME = "spring.ai.embedding";

    private static final KeyValue MODEL_NONE = KeyValue.of(SemanticConventions.EMBEDDING_MODEL_NAME, KeyValue.NONE_VALUE);

    private final OpenInferenceTracingOptions tracingOptions;

    public OpenInferenceEmbeddingModelObservationConvention(OpenInferenceTracingOptions tracingOptions) {
        this.tracingOptions = tracingOptions;
    }

    @Override
    public String getName() {
        return DEFAULT_NAME;
    }

    @Override
    public String getContextualName(EmbeddingModelObservationContext context) {
        if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getModel())) {
            return "%s %s".formatted(context.getOperationMetadata().operationType(), context.getRequest().getOptions().getModel());
        }
        return context.getOperationMetadata().operationType();
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(EmbeddingModelObservationContext context) {
        return KeyValues.of(aiOperationType(), aiProvider(context), llmSystem(context), embeddingModelName(context));
    }

    private KeyValue aiOperationType() {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.EMBEDDING.getValue());
    }

    private KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(SemanticConventions.LLM_PROVIDER,
                OpenInferenceConventionsConverters.toLlmProvider(context.getOperationMetadata().provider()));
    }

    private KeyValue llmSystem(EmbeddingModelObservationContext context) {
        return KeyValue.of(SemanticConventions.LLM_SYSTEM,
                OpenInferenceConventionsConverters.toLlmSystem(context.getOperationMetadata().provider()));
    }

    private KeyValue embeddingModelName(EmbeddingModelObservationContext context) {
        if (context.getResponse() != null && context.getResponse().getMetadata() != null
                && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            return KeyValue.of(SemanticConventions.EMBEDDING_MODEL_NAME, context.getResponse().getMetadata().getModel());
        } else if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getModel())) {
            return KeyValue.of(SemanticConventions.EMBEDDING_MODEL_NAME, context.getRequest().getOptions().getModel());
        }
        return MODEL_NONE;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(EmbeddingModelObservationContext context) {
        var keyValues = KeyValues.empty();

        // Request
        keyValues = embeddingsTexts(keyValues, context);
        keyValues = llmInvocationParameters(keyValues, context);

        // Response
        keyValues = embeddingsVectors(keyValues, context);
        keyValues = usageInputTokens(keyValues, context);
        keyValues = usageTotalTokens(keyValues, context);

        return keyValues;
    }

    // Request

    private KeyValues embeddingsTexts(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            return keyValues;
        }

        List<String> embeddingTexts = new ArrayList<>(context.getRequest().getInstructions());
        for (int i = 0; i < embeddingTexts.size(); i++) {
            String embeddingText = embeddingTexts.get(i);
            if (tracingOptions.isHideInputs() || tracingOptions.isHideInputMessages() || tracingOptions.isHideInputText()) {
                keyValues = keyValues.and(
                        SemanticConventions.EMBEDDING_EMBEDDINGS + "." + i + "." + SemanticConventions.EMBEDDING_TEXT,
                        OpenInferenceTracingOptions.REDACTED_PLACEHOLDER
                );
            } else {
                keyValues = keyValues.and(
                        SemanticConventions.EMBEDDING_EMBEDDINGS + "." + i + "." + SemanticConventions.EMBEDDING_TEXT,
                        embeddingText != null ? embeddingText : ""
                );
            }
        }

        return keyValues;
    }

    private KeyValues llmInvocationParameters(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (tracingOptions.isHideLlmInvocationParameters()) {
            return keyValues.and(SemanticConventions.LLM_INVOCATION_PARAMETERS, OpenInferenceTracingOptions.REDACTED_PLACEHOLDER);
        }

        EmbeddingOptions options = context.getRequest().getOptions();
        Map<String, Object> invocationParameters = new HashMap<>();

        if (options.getDimensions() != null) {
            invocationParameters.put("dimensions", options.getDimensions());
        }

        return keyValues.and(SemanticConventions.LLM_INVOCATION_PARAMETERS, JsonParser.toJson(invocationParameters));
    }

    // Response

    private KeyValues embeddingsVectors(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }

        List<Embedding> embeddings = new ArrayList<>(context.getResponse().getResults());
        for (int i = 0; i < embeddings.size(); i++) {
            Embedding embedding = embeddings.get(i);
            if (tracingOptions.isHideOutputs() || tracingOptions.isHideOutputMessages() || tracingOptions.isHideEmbeddingVectors()) {
                keyValues = keyValues.and(
                        SemanticConventions.EMBEDDING_EMBEDDINGS + "." + i + "." + SemanticConventions.EMBEDDING_VECTOR,
                        OpenInferenceTracingOptions.REDACTED_PLACEHOLDER
                );
            } else {
                keyValues = keyValues.and(
                        SemanticConventions.EMBEDDING_EMBEDDINGS + "." + i + "." + SemanticConventions.EMBEDDING_VECTOR,
                        "<%s dimensional vector>".formatted(embedding.getOutput().length)
                );
            }
        }

        return keyValues;
    }

    private KeyValues usageInputTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (context.getResponse() != null && context.getResponse().getMetadata() != null
                && context.getResponse().getMetadata().getUsage() != null
                && context.getResponse().getMetadata().getUsage().getPromptTokens() != null) {
            return keyValues.and(
                    SemanticConventions.LLM_TOKEN_COUNT_PROMPT,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getPromptTokens()));
        }
        return keyValues;
    }

    private KeyValues usageTotalTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (context.getResponse() != null && context.getResponse().getMetadata() != null
                && context.getResponse().getMetadata().getUsage() != null
                && context.getResponse().getMetadata().getUsage().getTotalTokens() != null) {
            return keyValues.and(
                    SemanticConventions.LLM_TOKEN_COUNT_TOTAL,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getTotalTokens()));
        }
        return keyValues;
    }

}
