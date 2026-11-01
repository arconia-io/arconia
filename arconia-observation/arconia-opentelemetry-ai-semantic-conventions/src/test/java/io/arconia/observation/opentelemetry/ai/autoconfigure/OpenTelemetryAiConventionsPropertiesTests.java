package io.arconia.observation.opentelemetry.ai.autoconfigure;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.arconia.observation.opentelemetry.ai.autoconfigure.OpenTelemetryAiConventionsProperties.InferenceContentDestination;
import io.arconia.observation.opentelemetry.ai.instrumentation.GenAiConventionFlavor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryAiConventionsProperties}.
 */
class OpenTelemetryAiConventionsPropertiesTests {

    private final OpenTelemetryAiConventionsProperties properties = new OpenTelemetryAiConventionsProperties();

    @Nested
    class ContentCapturing {

        @ParameterizedTest
        @EnumSource(GenAiConventionFlavor.class)
        void shouldCaptureContentByDefaultAsTheFlavorDoes(GenAiConventionFlavor flavor) {
            properties.setFlavor(flavor);
            boolean expected = flavor != GenAiConventionFlavor.OPENTELEMETRY;

            assertThat(properties.capturesMessageContent()).isEqualTo(expected);
            assertThat(properties.capturesToolDefinitions()).isEqualTo(expected);
            assertThat(properties.capturesToolCallContent()).isEqualTo(expected);
        }

        @ParameterizedTest
        @EnumSource(GenAiConventionFlavor.class)
        void shouldCaptureContentAsConfigured(GenAiConventionFlavor flavor) {
            properties.setFlavor(flavor);
            boolean configured = flavor == GenAiConventionFlavor.OPENTELEMETRY;
            properties.setCaptureMessageContent(configured);
            properties.setCaptureToolDefinitions(configured);
            properties.setCaptureToolCallContent(configured);

            assertThat(properties.capturesMessageContent()).isEqualTo(configured);
            assertThat(properties.capturesToolDefinitions()).isEqualTo(configured);
            assertThat(properties.capturesToolCallContent()).isEqualTo(configured);
        }

    }

    @Nested
    class ContentDestination {

        @ParameterizedTest
        @EnumSource(InferenceContentDestination.class)
        void shouldUseConfiguredDestinationForOpenTelemetryFlavor(InferenceContentDestination destination) {
            properties.setInferenceContentDestination(destination);

            assertThat(properties.contentDestination()).isEqualTo(destination);
        }

        @ParameterizedTest
        @EnumSource(value = GenAiConventionFlavor.class, names = "OPENTELEMETRY", mode = EnumSource.Mode.EXCLUDE)
        void shouldUseSpanAttributesForOtherFlavors(GenAiConventionFlavor flavor) {
            properties.setFlavor(flavor);
            properties.setInferenceContentDestination(InferenceContentDestination.LOG_EVENTS);

            assertThat(properties.contentDestination()).isEqualTo(InferenceContentDestination.SPAN_ATTRIBUTES);
        }

    }

}
