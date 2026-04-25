package io.arconia.observation.opentelemetry.instrumentation.genai;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.Observation;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.chat.metadata.Usage;

/**
 * Utility to generate metrics for token usage.
 */
final class OpenTelemetryTokenUsageMetricsGenerator {

    private static final String DESCRIPTION = "Number of input and output tokens used.";

    private static final String BASE_UNIT = "{token}";

    private OpenTelemetryTokenUsageMetricsGenerator() {
    }

    public static void inputTokens(Usage usage, Observation.Context context, MeterRegistry meterRegistry) {
        DistributionSummary.builder(GenAiMoreIncubatingAttributes.GEN_AI_CLIENT_TOKEN_USAGE)
                .tag(GenAiIncubatingAttributes.GEN_AI_TOKEN_TYPE.getKey(), GenAiIncubatingAttributes.GenAiTokenTypeIncubatingValues.INPUT)
                .description(DESCRIPTION)
                .baseUnit(BASE_UNIT)
                .tags(createTags(context))
                .register(meterRegistry)
                .record(usage.getPromptTokens());
    }

    public static void outputTokens(Usage usage, Observation.Context context, MeterRegistry meterRegistry) {
        DistributionSummary.builder(GenAiMoreIncubatingAttributes.GEN_AI_CLIENT_TOKEN_USAGE)
                .tag(GenAiIncubatingAttributes.GEN_AI_TOKEN_TYPE.getKey(), GenAiIncubatingAttributes.GenAiTokenTypeIncubatingValues.OUTPUT)
                .description(DESCRIPTION)
                .baseUnit(BASE_UNIT)
                .tags(createTags(context))
                .register(meterRegistry)
                .record(usage.getCompletionTokens());
    }

    private static List<Tag> createTags(Observation.Context context) {
        List<Tag> tags = new ArrayList<>();
        for (KeyValue keyValue : context.getLowCardinalityKeyValues()) {
            tags.add(Tag.of(keyValue.getKey(), keyValue.getValue()));
        }
        return tags;
    }

}
