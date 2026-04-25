package io.arconia.observation.opentelemetry.instrumentation.genai;

import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.util.Assert;

final class OpenTelemetryGenAiConventionsConverter {

    static String toOperationName(String aiOperationType) {
        Assert.hasText(aiOperationType, "aiOperationType cannot be null or empty");

        try {
            AiOperationType aiOperationTypeEnum = AiOperationType.valueOf(aiOperationType.toUpperCase().strip());
            return switch(aiOperationTypeEnum) {
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

    static String toProviderName(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");

        try {
            AiProvider aiProviderEnum = AiProvider.valueOf(aiProvider.toUpperCase().strip());
            return switch(aiProviderEnum) {
                case AiProvider.ANTHROPIC -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.ANTHROPIC;
                case AiProvider.AZURE_OPENAI -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.AZURE_AI_OPENAI;
                case AiProvider.BEDROCK_CONVERSE -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.AWS_BEDROCK;
                case AiProvider.DEEPSEEK -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.DEEPSEEK;
                case AiProvider.GOOGLE_GENAI_AI -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.GCP_GEN_AI;
                case AiProvider.MISTRAL_AI -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.MISTRAL_AI;
                case AiProvider.OPENAI, AiProvider.OPENAI_SDK -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.OPENAI;
                case AiProvider.VERTEX_AI -> GenAiIncubatingAttributes.GenAiProviderNameIncubatingValues.GCP_VERTEX_AI;
                default -> aiProviderEnum.value();
            };
        } catch (IllegalArgumentException e) {
            return aiProvider;
        }
    }

}
