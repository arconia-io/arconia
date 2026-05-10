package io.arconia.observation.opentelemetry.ai.instrumentation.shared;

import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.util.Assert;

/**
 * Converters from Spring AI observation conventions to GenAI semantic convention values,
 * covering all supported flavors (OpenTelemetry, OpenLLMetry, LangSmith).
 */
public final class GenAiConventionsConverter {

    private GenAiConventionsConverter() {}

    /**
     * Converts an {@link AiOperationType} value to an OTel GenAI operation name.
     */
    public static String toOperationName(String aiOperationType) {
        Assert.hasText(aiOperationType, "aiOperationType cannot be null or empty");
        try {
            AiOperationType type = AiOperationType.valueOf(aiOperationType.toUpperCase().strip());
            return switch (type) {
                case AiOperationType.CHAT -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.CHAT;
                case AiOperationType.EMBEDDING -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.EMBEDDINGS;
                case AiOperationType.IMAGE -> AiOperationType.IMAGE.value();
                case AiOperationType.TEXT_COMPLETION -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.TEXT_COMPLETION;
                case AiOperationType.EXECUTE_TOOL -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.EXECUTE_TOOL;
                default -> type.value();
            };
        } catch (IllegalArgumentException e) {
            return aiOperationType;
        }
    }

    /**
     * Converts an {@link AiProvider} value to an OTel {@code gen_ai.provider.name} value.
     */
    public static String toProviderName(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");
        try {
            AiProvider provider = AiProvider.valueOf(aiProvider.toUpperCase().strip());
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
        } catch (IllegalArgumentException e) {
            return aiProvider;
        }
    }

    /**
     * Converts an {@link AiOperationType} value to a {@code langsmith.span.kind} value.
     */
    public static String toLangSmithSpanKind(String aiOperationType) {
        Assert.hasText(aiOperationType, "aiOperationType cannot be null or empty");
        try {
            AiOperationType type = AiOperationType.valueOf(aiOperationType.toUpperCase().strip());
            return switch (type) {
                case AiOperationType.CHAT, AiOperationType.TEXT_COMPLETION -> "llm";
                case AiOperationType.EMBEDDING -> "embedding";
                case AiOperationType.IMAGE -> "image";
                case AiOperationType.EXECUTE_TOOL -> "tool";
                default -> "chain";
            };
        } catch (IllegalArgumentException e) {
            return "chain";
        }
    }

}
