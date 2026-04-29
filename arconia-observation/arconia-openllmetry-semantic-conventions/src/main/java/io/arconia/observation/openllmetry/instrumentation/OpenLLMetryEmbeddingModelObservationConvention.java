package io.arconia.observation.openllmetry.instrumentation;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationContext;
import org.springframework.ai.util.json.JsonParser;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention} for OpenLLMetry.
 *
 * @see <a href="https://github.com/traceloop/openllmetry">OpenLLMetry</a>
 */
public final class OpenLLMetryEmbeddingModelObservationConvention extends DefaultEmbeddingModelObservationConvention {

    private static final KeyValue MODEL_NONE = KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, KeyValue.NONE_VALUE);

    private final OpenLLMetryOptions openLLMetryOptions;

    public OpenLLMetryEmbeddingModelObservationConvention(OpenLLMetryOptions openLLMetryOptions) {
        this.openLLMetryOptions = openLLMetryOptions;
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(EmbeddingModelObservationContext context) {
        return KeyValues.of(traceloopSpanKind(), aiProvider(context), genAiOperationName(),
                genAiRequestModel(context));
    }

    private KeyValue traceloopSpanKind() {
        return KeyValue.of(OpenLLMetryAttributes.TRACELOOP_SPAN_KIND, OpenLLMetryAttributes.SPAN_KIND_TASK);
    }

    protected KeyValue aiProvider(EmbeddingModelObservationContext context) {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_SYSTEM,
                OpenLLMetryConventionsConverters.toSystemName(context.getOperationMetadata().provider()));
    }

    private KeyValue genAiOperationName() {
        return KeyValue.of(OpenLLMetryAttributes.GEN_AI_OPERATION_NAME, OpenLLMetryAttributes.OPERATION_EMBEDDINGS);
    }

    private KeyValue genAiRequestModel(EmbeddingModelObservationContext context) {
        if (context.getResponse() != null && StringUtils.hasText(context.getResponse().getMetadata().getModel())) {
            return KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, context.getResponse().getMetadata().getModel());
        } else if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getModel())) {
            return KeyValue.of(OpenLLMetryAttributes.GEN_AI_REQUEST_MODEL, context.getRequest().getOptions().getModel());
        }
        return MODEL_NONE;
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(EmbeddingModelObservationContext context) {
        var keyValues = KeyValues.empty();

        // Request
        keyValues = entityInput(keyValues, context);

        // Response
        keyValues = entityOutput(keyValues, context);
        keyValues = usageInputTokens(keyValues, context);
        keyValues = usageTotalTokens(keyValues, context);

        return keyValues;
    }

    // Request

    private KeyValues entityInput(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
            return keyValues;
        }

        if (!openLLMetryOptions.isTraceContent()) {
            return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER);
        }

        List<String> embeddingTexts = new ArrayList<>(context.getRequest().getInstructions());
        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_INPUT, JsonParser.toJson(embeddingTexts));
    }

    // Response

    private KeyValues entityOutput(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (context.getResponse() == null || CollectionUtils.isEmpty(context.getResponse().getResults())) {
            return keyValues;
        }

        if (!openLLMetryOptions.isTraceContent()) {
            return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, OpenLLMetryOptions.REDACTED_PLACEHOLDER);
        }

        List<Embedding> embeddings = new ArrayList<>(context.getResponse().getResults());
        List<String> vectors = new ArrayList<>();
        for (Embedding embedding : embeddings) {
            vectors.add("<%s dimensional vector>".formatted(embedding.getOutput().length));
        }

        return keyValues.and(OpenLLMetryAttributes.TRACELOOP_ENTITY_OUTPUT, JsonParser.toJson(vectors));
    }

    protected KeyValues usageInputTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(OpenLLMetryAttributes.GEN_AI_USAGE_INPUT_TOKENS,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getPromptTokens()));
        }
        return keyValues;
    }

    protected KeyValues usageTotalTokens(KeyValues keyValues, EmbeddingModelObservationContext context) {
        if (context.getResponse() != null) {
            return keyValues.and(OpenLLMetryAttributes.GEN_AI_USAGE_TOTAL_TOKENS,
                    String.valueOf(context.getResponse().getMetadata().getUsage().getTotalTokens()));
        }
        return keyValues;
    }

}
