package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.ai.observation.conventions.AiProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenLitImageModelObservationConvention}.
 */
class OpenLitImageModelObservationConventionTests {

    private final OpenLitImageModelObservationConvention convention =
            new OpenLitImageModelObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(OpenLitImageModelObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameWhenModelIsDefined() {
        ImageModelObservationContext context = createContext("dall-e-3");
        assertThat(convention.getContextualName(context)).isEqualTo("image dall-e-3");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        ImageModelObservationContext context = createContext("dall-e-3");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "image"),
                KeyValue.of(OpenLitAttributes.GEN_AI_SYSTEM, "openai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), "dall-e-3")
        );
    }

    @Test
    void shouldNotHaveOtelProviderNameKey() {
        ImageModelObservationContext context = createContext("dall-e-3");

        assertThat(convention.getLowCardinalityKeyValues(context)).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey()));
    }

    @Test
    void shouldUseImageSizeKeyWithUnderscoreSeparator() {
        ImagePrompt prompt = new ImagePrompt("A sunset over the ocean",
                ImageOptionsBuilder.builder()
                        .model("dall-e-3")
                        .width(1024)
                        .height(1024)
                        .build());
        ImageModelObservationContext context = ImageModelObservationContext.builder()
                .imagePrompt(prompt)
                .provider(AiProvider.OPENAI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(OpenLitAttributes.GEN_AI_REQUEST_IMAGE_SIZE, "1024x1024")
        );
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo("gen_ai.request.image.size"));
    }

    @Test
    void shouldUseImageStyleKeyWithUnderscoreSeparator() {
        ImagePrompt prompt = new ImagePrompt("A sunset over the ocean",
                ImageOptionsBuilder.builder()
                        .model("dall-e-3")
                        .style("vivid")
                        .build());
        ImageModelObservationContext context = ImageModelObservationContext.builder()
                .imagePrompt(prompt)
                .provider(AiProvider.OPENAI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(OpenLitAttributes.GEN_AI_REQUEST_IMAGE_STYLE, "vivid")
        );
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo("gen_ai.request.image.style"));
    }

    @Test
    void shouldNotIncludeImageSizeWhenNotSet() {
        ImageModelObservationContext context = createContext("dall-e-3");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLitAttributes.GEN_AI_REQUEST_IMAGE_SIZE));
    }

    @Test
    void shouldNotIncludeImageStyleWhenNotSet() {
        ImageModelObservationContext context = createContext("dall-e-3");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).noneSatisfy(kv ->
                assertThat(kv.getKey()).isEqualTo(OpenLitAttributes.GEN_AI_REQUEST_IMAGE_STYLE));
    }

    @Test
    void shouldAlwaysHaveOutputTypeImage() {
        ImageModelObservationContext context = createContext("dall-e-3");

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE.getKey(),
                        GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.IMAGE));
    }

    // Helpers

    private ImageModelObservationContext createContext(String model) {
        ImagePrompt prompt = new ImagePrompt("A sunset over the ocean",
                ImageOptionsBuilder.builder().model(model).build());
        return ImageModelObservationContext.builder()
                .imagePrompt(prompt)
                .provider(AiProvider.OPENAI.value())
                .build();
    }

}
