package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.util.Assert;

/**
 * Converters from Spring AI observation conventions to GenAI semantic convention values,
 * covering all the flavors (OpenTelemetry, OpenLLMetry, LangSmith, OpenLIT, Langfuse).
 */
public final class GenAiConventionsConverter {

    /**
     * Spring AI reports the value of an operation type or provider, such as {@code google_genai}, which doesn't
     * always match the constant name. Both are indexed once, in lower case, so that a lookup is a map access
     * rather than a scan or an exception for the unknown values that custom providers report.
     */
    private static final Map<String, AiOperationType> OPERATION_TYPES = index(AiOperationType.values(), AiOperationType::value);

    private static final Map<String, AiProvider> PROVIDERS = index(AiProvider.values(), AiProvider::value);

    private GenAiConventionsConverter() {}

    /**
     * Converts an {@link AiOperationType} value to an OTel GenAI operation name.
     */
    public static String toOperationName(String aiOperationType) {
        Assert.hasText(aiOperationType, "aiOperationType cannot be null or empty");
        AiOperationType type = findOperationType(aiOperationType);
        if (type == null) {
            return aiOperationType;
        }
        return switch (type) {
            case AiOperationType.CHAT -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.CHAT;
            case AiOperationType.EMBEDDING -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.EMBEDDINGS;
            case AiOperationType.IMAGE -> AiOperationType.IMAGE.value();
            case AiOperationType.TEXT_COMPLETION -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.TEXT_COMPLETION;
            case AiOperationType.EXECUTE_TOOL -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.EXECUTE_TOOL;
            default -> type.value();
        };
    }

    /**
     * Converts an {@link AiProvider} value to an OTel {@code gen_ai.provider.name} value.
     */
    public static String toProviderName(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");
        AiProvider provider = findProvider(aiProvider);
        if (provider == null) {
            return aiProvider;
        }
        return switch (provider) {
            case AiProvider.ANTHROPIC -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.ANTHROPIC;
            case AiProvider.BEDROCK_CONVERSE -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.AWS_BEDROCK;
            case AiProvider.DEEPSEEK -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.DEEPSEEK;
            case AiProvider.GOOGLE_GENAI_AI -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.GCP_GEN_AI;
            case AiProvider.MISTRAL_AI -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.MISTRAL_AI;
            case AiProvider.OPENAI -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.OPENAI;
            case AiProvider.VERTEX_AI -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.GCP_VERTEX_AI;
            default -> provider.value();
        };
    }

    /**
     * Converts an {@link AiProvider} value to a {@code gen_ai.provider.name} value recognized by OpenLIT.
     * It differs from {@link #toProviderName(String)} only for Google GenAI, reported as {@code gcp.gemini},
     * which OpenLIT recognizes, instead of {@code gcp.gen_ai}.
     */
    public static String toOpenLitProviderName(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");
        if (findProvider(aiProvider) == AiProvider.GOOGLE_GENAI_AI) {
            return GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.GCP_GEMINI;
        }
        return toProviderName(aiProvider);
    }

    /**
     * Converts an {@link AiProvider} value to a {@code gen_ai.system} value recognized by LangSmith.
     * It differs from {@link #toProviderName(String)} only for Google GenAI, reported as {@code google},
     * as the LangSmith SDKs do, instead of {@code gcp.gen_ai}.
     */
    public static String toLangSmithProviderName(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");
        if (findProvider(aiProvider) == AiProvider.GOOGLE_GENAI_AI) {
            return "google";
        }
        return toProviderName(aiProvider);
    }

    /**
     * Finds the {@link AiProvider} matching the given value, such as {@code google_genai}, or constant name.
     */
    private static @Nullable AiProvider findProvider(String aiProvider) {
        return PROVIDERS.get(aiProvider.strip().toLowerCase(Locale.ROOT));
    }

    /**
     * Finds the {@link AiOperationType} matching the given value, such as {@code execute_tool}, or constant name.
     */
    private static @Nullable AiOperationType findOperationType(String aiOperationType) {
        return OPERATION_TYPES.get(aiOperationType.strip().toLowerCase(Locale.ROOT));
    }

    /**
     * Indexes the constants of an enum by their lower-case value and name.
     */
    private static <E extends Enum<E>> Map<String, E> index(E[] constants, Function<E, String> value) {
        Map<String, E> index = new HashMap<>();
        for (E constant : constants) {
            index.put(value.apply(constant).toLowerCase(Locale.ROOT), constant);
            index.put(constant.name().toLowerCase(Locale.ROOT), constant);
        }
        return Map.copyOf(index);
    }

    /**
     * Converts an {@link AiOperationType} value to a {@code langsmith.span.kind} value.
     */
    public static String toLangSmithSpanKind(String aiOperationType) {
        Assert.hasText(aiOperationType, "aiOperationType cannot be null or empty");
        AiOperationType type = findOperationType(aiOperationType);
        if (type == null) {
            return "chain";
        }
        return switch (type) {
            case AiOperationType.CHAT, AiOperationType.TEXT_COMPLETION, AiOperationType.IMAGE -> "llm";
            case AiOperationType.EMBEDDING -> "embedding";
            case AiOperationType.EXECUTE_TOOL -> "tool";
            default -> "chain";
        };
    }


    /**
     * Converts a finish reason reported by a model provider to the corresponding well-known value
     * of the GenAI semantic conventions ({@code stop}, {@code length}, {@code content_filter},
     * {@code tool_call}, {@code error}), returning the input for values without a corresponding one.
     */
    public static String toFinishReason(String finishReason) {
        Assert.hasText(finishReason, "finishReason cannot be null or empty");
        return switch (finishReason.toLowerCase(Locale.ROOT).strip()) {
            case "stop", "end_turn", "stop_sequence" -> "stop";
            case "length", "max_tokens", "model_length" -> "length";
            case "content_filter", "content_filtered", "guardrail_intervened", "safety", "recitation", "blocklist",
                 "prohibited_content", "spii" -> "content_filter";
            case "tool_call", "tool_calls", "tool_use", "function_call" -> "tool_call";
            case "error" -> "error";
            default -> finishReason;
        };
    }

    /**
     * Removes the key values set to {@code none}, the placeholder Spring AI uses when a value isn't available,
     * such as the model of a failed request: the conventions require such attributes only when available.
     */
    public static KeyValues withoutNoneValues(KeyValues keyValues) {
        return KeyValues.of(keyValues.stream()
                .filter(keyValue -> !KeyValue.NONE_VALUE.equals(keyValue.getValue()))
                .toList());
    }

}
