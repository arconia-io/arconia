package io.arconia.observation.openllmetry.instrumentation;

import org.springframework.ai.observation.conventions.AiProvider;
import org.springframework.util.Assert;

/**
 * Converters from Spring AI to OpenLLMetry conventions.
 */
final class OpenLLMetryConventionsConverters {

    /**
     * Convert the AI provider to the OpenLLMetry {@code gen_ai.system} value.
     */
    static String toSystemName(String aiProvider) {
        Assert.hasText(aiProvider, "aiProvider cannot be null or empty");

        try {
            AiProvider aiProviderEnum = AiProvider.valueOf(aiProvider.toUpperCase().strip());
            return switch (aiProviderEnum) {
                case AiProvider.ANTHROPIC -> OpenLLMetryAttributes.PROVIDER_ANTHROPIC;
                case AiProvider.AZURE_OPENAI -> OpenLLMetryAttributes.PROVIDER_AZURE_AI_OPENAI;
                case AiProvider.BEDROCK_CONVERSE -> OpenLLMetryAttributes.PROVIDER_AWS_BEDROCK;
                case AiProvider.DEEPSEEK -> OpenLLMetryAttributes.PROVIDER_DEEPSEEK;
                case AiProvider.GOOGLE_GENAI_AI -> OpenLLMetryAttributes.PROVIDER_GCP_GEN_AI;
                case AiProvider.MISTRAL_AI -> OpenLLMetryAttributes.PROVIDER_MISTRAL_AI;
                case AiProvider.OPENAI, AiProvider.OPENAI_SDK -> OpenLLMetryAttributes.PROVIDER_OPENAI;
                case AiProvider.VERTEX_AI -> OpenLLMetryAttributes.PROVIDER_GCP_VERTEX_AI;
                default -> aiProviderEnum.value();
            };
        } catch (IllegalArgumentException e) {
            return aiProvider;
        }
    }

}
