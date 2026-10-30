package io.arconia.observation.openinference.ai.instrumentation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.util.JsonHelper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EmbeddingModelObservationConvention} for OpenInference.
 */
public final class OpenInferenceEmbeddingModelObservationConvention extends DefaultEmbeddingModelObservationConvention {

    private final JsonHelper jsonHelper = new JsonHelper();

    private final OpenInferenceOptions openInferenceOptions;

    public OpenInferenceEmbeddingModelObservationConvention(OpenInferenceOptions openInferenceOptions) {
        this.openInferenceOptions = openInferenceOptions;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(EmbeddingModelObservationContext context) {
        KeyValues keyValues = KeyValues.of(aiOperationType(context), aiProvider(context), llmSystem(context));
        String modelName = modelName(context);
        return modelName != null ? keyValues.and(SemanticConventions.EMBEDDING_MODEL_NAME, modelName) : keyValues;
    }

    @Override
    protected KeyValue aiOperationType(EmbeddingModelObservationContext context) {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.EMBEDDING.getValue());
    }

    @Override
    protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(SemanticConventions.LLM_PROVIDER,
                OpenInferenceConventionsConverters.toLlmProvider(context.getOperationMetadata().provider()));
    }

    private KeyValue llmSystem(EmbeddingModelObservationContext context) {
        return KeyValue.of(SemanticConventions.LLM_SYSTEM,
                OpenInferenceConventionsConverters.toLlmSystem(context.getOperationMetadata().provider()));
    }

    private @Nullable String modelName(EmbeddingModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            return context.getResponse().getMetadata().getModel();
        } else if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getModel())) {
            return context.getRequest().getOptions().getModel();
        }
        return null;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(EmbeddingModelObservationContext context) {
        var keyValues = KeyValues.empty();

        // Request
        keyValues = embeddingsTexts(keyValues, context);
        keyValues = embeddingInvocationParameters(keyValues, context);

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
            if (openInferenceOptions.isHideInputs() || openInferenceOptions.isHideEmbeddingsText()) {
                keyValues = keyValues.and(
                        SemanticConventions.EMBEDDING_EMBEDDINGS + "." + i + "." + SemanticConventions.EMBEDDING_TEXT,
                        OpenInferenceOptions.REDACTED_PLACEHOLDER
                );
            } else {
                keyValues = keyValues.and(
                        SemanticConventions.EMBEDDING_EMBEDDINGS + "." + i + "." + SemanticConventions.EMBEDDING_TEXT,
                        embeddingText
                );
            }
        }

        return keyValues;
    }

    private KeyValues embeddingInvocationParameters(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (openInferenceOptions.isHideLlmInvocationParameters()) {
            return keyValues.and(SemanticConventions.EMBEDDING_INVOCATION_PARAMETERS, OpenInferenceOptions.REDACTED_PLACEHOLDER);
        }

        EmbeddingOptions options = context.getRequest().getOptions();
        if (options == null) {
            return keyValues;
        }

        Map<String, Object> invocationParameters = new HashMap<>();

        if (options.getDimensions() != null) {
            invocationParameters.put("dimensions", options.getDimensions());
        }

        return keyValues.and(SemanticConventions.EMBEDDING_INVOCATION_PARAMETERS, jsonHelper.toJson(invocationParameters));
    }

    // Response

    private KeyValues embeddingsVectors(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }

        List<Embedding> embeddings = new ArrayList<>(context.getResponse().getResults());
        for (int i = 0; i < embeddings.size(); i++) {
            Embedding embedding = embeddings.get(i);
            if (openInferenceOptions.isHideOutputs() || openInferenceOptions.isHideEmbeddingsVectors()) {
                keyValues = keyValues.and(
                        SemanticConventions.EMBEDDING_EMBEDDINGS + "." + i + "." + SemanticConventions.EMBEDDING_VECTOR,
                        OpenInferenceOptions.REDACTED_PLACEHOLDER
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

    @Override
    protected KeyValues usageInputTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
        Usage usage = reportedUsage(context);
        if (usage != null && usage.getPromptTokens() != null) {
            return keyValues.and(SemanticConventions.LLM_TOKEN_COUNT_PROMPT, String.valueOf(usage.getPromptTokens()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues usageTotalTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
        Usage usage = reportedUsage(context);
        if (usage != null && usage.getTotalTokens() != null) {
            return keyValues.and(SemanticConventions.LLM_TOKEN_COUNT_TOTAL, String.valueOf(usage.getTotalTokens()));
        }
        return keyValues;
    }

    /**
     * Returns the token usage of the response when the provider reported it. Spring AI falls back to
     * {@link EmptyUsage} when it didn't, and its zeros would show up as free calls in the token counts.
     */
    private static @Nullable Usage reportedUsage(EmbeddingModelObservationContext context) {
        if (context.getResponse() == null) {
            return null;
        }
        Usage usage = context.getResponse().getMetadata().getUsage();
        return usage instanceof EmptyUsage ? null : usage;
    }

}
