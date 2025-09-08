package io.arconia.openinference.observation.instrumentation;

import com.arize.semconv.trace.SemanticConventions;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationContext;
import org.springframework.ai.chat.client.advisor.observation.AdvisorObservationConvention;
import org.springframework.ai.util.ParsingUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@link AdvisorObservationConvention} for OpenInference.
 */
public class OpenInferenceAdvisorObservationConvention implements AdvisorObservationConvention {

    public static final String DEFAULT_NAME = "spring.ai.advisor";

    @Override
    public String getName() {
        return DEFAULT_NAME;
    }

    @Override
    @Nullable
    public String getContextualName(AdvisorObservationContext context) {
        Assert.notNull(context, "context cannot be null");
        return ParsingUtils.reConcatenateCamelCase(context.getAdvisorName(), "_")
                .replace("_advisor", "");
    }

    @Override
    public KeyValues getLowCardinalityKeyValues(AdvisorObservationContext context) {
        Assert.notNull(context, "context cannot be null");
        return KeyValues.of(aiOperationType());
    }

    private KeyValue aiOperationType() {
        return KeyValue.of(SemanticConventions.OPENINFERENCE_SPAN_KIND, SemanticConventions.OpenInferenceSpanKind.CHAIN.getValue());
    }

}
