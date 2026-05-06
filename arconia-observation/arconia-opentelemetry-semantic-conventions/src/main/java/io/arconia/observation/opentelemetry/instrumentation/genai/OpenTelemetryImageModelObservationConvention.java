package io.arconia.observation.opentelemetry.instrumentation.genai;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;

import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationContext;

public class OpenTelemetryImageModelObservationConvention extends DefaultImageModelObservationConvention {

    @Override
    protected KeyValue aiOperationType(ImageModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME.getKey(),
                OpenTelemetryGenAiConventionsConverter.toOperationName(context.getOperationMetadata().operationType()));
    }

    @Override
    protected KeyValue aiProvider(ImageModelObservationContext context) {
        return KeyValue.of(GenAiIncubatingAttributes.GEN_AI_PROVIDER_NAME.getKey(),
                OpenTelemetryGenAiConventionsConverter.toProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    public KeyValues getHighCardinalityKeyValues(ImageModelObservationContext context) {
        var keyValues = super.getHighCardinalityKeyValues(context);
        keyValues = outputType(keyValues, context);
        return keyValues;
    }

    private KeyValues outputType(KeyValues keyValues, ImageModelObservationContext context) {
        return keyValues.and(GenAiIncubatingAttributes.GEN_AI_OUTPUT_TYPE.getKey(),
                GenAiIncubatingAttributes.GenAiOutputTypeIncubatingValues.IMAGE);
    }

}
