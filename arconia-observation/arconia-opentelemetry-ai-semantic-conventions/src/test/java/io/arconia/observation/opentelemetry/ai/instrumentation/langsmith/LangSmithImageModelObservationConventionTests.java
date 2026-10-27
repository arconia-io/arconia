package io.arconia.observation.opentelemetry.ai.instrumentation.langsmith;

import io.micrometer.common.KeyValue;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.ai.observation.conventions.AiProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LangSmithImageModelObservationConvention}.
 */
class LangSmithImageModelObservationConventionTests {

    private final LangSmithImageModelObservationConvention convention =
            new LangSmithImageModelObservationConvention();

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        ImageModelObservationContext context = createContext(AiProvider.OPENAI);

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "image"),
                KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(), "openai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), "dall-e-3"),
                KeyValue.of(LangSmithAttributes.LANGSMITH_SPAN_KIND.getKey(), "llm")
        );
    }

    @Test
    void shouldUseLangSmithProviderName() {
        ImageModelObservationContext context = createContext(AiProvider.GOOGLE_GENAI_AI);

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(LangSmithAttributes.GEN_AI_SYSTEM.getKey(), "google"));
        assertThat(convention.getLowCardinalityKeyValues(context)).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey()));
    }

    // Helpers

    private ImageModelObservationContext createContext(AiProvider provider) {
        ImagePrompt prompt = new ImagePrompt("A sunset over the ocean",
                ImageOptionsBuilder.builder().model("dall-e-3").build());
        return ImageModelObservationContext.builder()
                .imagePrompt(prompt)
                .provider(provider.value())
                .build();
    }

}
