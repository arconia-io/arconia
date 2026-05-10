package io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.ai.observation.conventions.AiObservationAttributes;
import org.springframework.ai.observation.conventions.AiProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryImageModelObservationConvention}.
 */
class OpenTelemetryImageModelObservationConventionTests {

    private final OpenTelemetryImageModelObservationConvention convention =
            new OpenTelemetryImageModelObservationConvention();

    @Test
    void shouldHaveName() {
        assertThat(convention.getName())
                .isEqualTo(OpenTelemetryImageModelObservationConvention.DEFAULT_NAME);
    }

    @Test
    void contextualNameWhenModelIsDefined() {
        ImageModelObservationContext context = createContext("dall-e-3");
        assertThat(convention.getContextualName(context)).isEqualTo("image dall-e-3");
    }

    @Test
    void contextualNameWhenModelIsNotDefined() {
        ImageModelObservationContext context = createContext(null);
        assertThat(convention.getContextualName(context)).isEqualTo("image");
    }

    @Test
    void shouldHaveLowCardinalityKeyValues() {
        ImageModelObservationContext context = createContext("dall-e-3");

        assertThat(convention.getLowCardinalityKeyValues(context)).contains(
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(), "image"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(), "openai"),
                KeyValue.of(GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL.getKey(), "dall-e-3")
        );
    }

    @Test
    void shouldHaveHighCardinalityKeyValuesWithRequestParams() {
        ImagePrompt prompt = new ImagePrompt("A sunset over the ocean",
                ImageOptionsBuilder.builder()
                        .model("dall-e-3")
                        .responseFormat("url")
                        .width(1024)
                        .height(1024)
                        .style("vivid")
                        .build());
        ImageModelObservationContext context = ImageModelObservationContext.builder()
                .imagePrompt(prompt)
                .provider(AiProvider.OPENAI.value())
                .build();

        KeyValues keyValues = convention.getHighCardinalityKeyValues(context);
        assertThat(keyValues).contains(
                KeyValue.of(AiObservationAttributes.REQUEST_IMAGE_RESPONSE_FORMAT.value(), "url"),
                KeyValue.of(AiObservationAttributes.REQUEST_IMAGE_SIZE.value(), "1024x1024"),
                KeyValue.of(AiObservationAttributes.REQUEST_IMAGE_STYLE.value(), "vivid")
        );
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
        var optionsBuilder = ImageOptionsBuilder.builder();
        if (model != null) {
            optionsBuilder.model(model);
        }
        ImagePrompt prompt = new ImagePrompt("A sunset over the ocean", optionsBuilder.build());
        return ImageModelObservationContext.builder()
                .imagePrompt(prompt)
                .provider(AiProvider.OPENAI.value())
                .build();
    }

}
