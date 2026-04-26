package io.arconia.observation.langsmith.instrumentation;

import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.util.Assert;

final class LangSmithConventionsConverter {

    /**
     * Converts an {@link AiProvider} value to a {@code gen_ai.system} value
     * recognized by LangSmith.
     */
    static String toSystemName(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");

        try {
            AiProvider aiProviderEnum = AiProvider.valueOf(aiProvider.toUpperCase().strip());
            return switch (aiProviderEnum) {
                case AiProvider.ANTHROPIC -> "anthropic";
                case AiProvider.AZURE_OPENAI -> "azure_openai";
                case AiProvider.BEDROCK_CONVERSE -> "aws_bedrock";
                case AiProvider.DEEPSEEK -> "deepseek";
                case AiProvider.GOOGLE_GENAI_AI -> "gcp_gen_ai";
                case AiProvider.MISTRAL_AI -> "mistral_ai";
                case AiProvider.OPENAI, AiProvider.OPENAI_SDK -> "openai";
                case AiProvider.VERTEX_AI -> "gcp_vertex_ai";
                default -> aiProviderEnum.value();
            };
        } catch (IllegalArgumentException e) {
            return aiProvider;
        }
    }

    /**
     * Converts an {@link AiOperationType} value to a {@code langsmith.span.kind} value
     * representing the LangSmith run type.
     */
    static String toSpanKind(String aiOperationType) {
        Assert.hasText(aiOperationType, "aiOperationType cannot be null or empty");

        try {
            AiOperationType aiOperationTypeEnum = AiOperationType.valueOf(aiOperationType.toUpperCase().strip());
            return switch (aiOperationTypeEnum) {
                case AiOperationType.CHAT, AiOperationType.TEXT_COMPLETION -> "llm";
                case AiOperationType.EMBEDDING -> "embedding";
                default -> "chain";
            };
        } catch (IllegalArgumentException e) {
            return "chain";
        }
    }

    /**
     * Converts an {@link AiOperationType} value to a GenAI operation name.
     */
    static String toOperationName(String aiOperationType) {
        Assert.hasText(aiOperationType, "aiOperationType cannot be null or empty");

        try {
            AiOperationType aiOperationTypeEnum = AiOperationType.valueOf(aiOperationType.toUpperCase().strip());
            return switch (aiOperationTypeEnum) {
                case AiOperationType.CHAT -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.CHAT;
                case AiOperationType.EMBEDDING -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.EMBEDDINGS;
                case AiOperationType.IMAGE -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.GENERATE_CONTENT;
                case AiOperationType.TEXT_COMPLETION -> GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues.TEXT_COMPLETION;
                default -> aiOperationTypeEnum.value();
            };
        } catch (IllegalArgumentException e) {
            return aiOperationType;
        }
    }

}
