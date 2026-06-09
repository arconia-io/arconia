package io.arconia.observation.opentelemetry.ai.instrumentation.openlit;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;

import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.util.StringUtils;

import io.arconia.observation.opentelemetry.ai.instrumentation.opentelemetry.OpenTelemetryImageModelObservationConvention;
import io.arconia.observation.opentelemetry.ai.instrumentation.shared.GenAiConventionsConverter;

/**
 * OpenLIT flavor of {@link OpenTelemetryImageModelObservationConvention}.
 *
 * @see <a href="https://github.com/openlit/openlit">OpenLIT</a>
 */
public final class OpenLitImageModelObservationConvention extends OpenTelemetryImageModelObservationConvention {

    @Override
    protected KeyValue aiProvider(ImageModelObservationContext context) {
        return KeyValue.of(OpenLitAttributes.GEN_AI_SYSTEM,
                GenAiConventionsConverter.toOpenLitProviderName(context.getOperationMetadata().provider()));
    }

    @Override
    protected KeyValues requestImageSize(KeyValues keyValues, ImageModelObservationContext context) {
        if (context.getRequest().getOptions() != null && context.getRequest().getOptions().getWidth() != null
                && context.getRequest().getOptions().getHeight() != null) {
            return keyValues.and(OpenLitAttributes.GEN_AI_REQUEST_IMAGE_SIZE,
                    "%sx%s".formatted(context.getRequest().getOptions().getWidth(),
                            context.getRequest().getOptions().getHeight()));
        }
        return keyValues;
    }

    @Override
    protected KeyValues requestImageStyle(KeyValues keyValues, ImageModelObservationContext context) {
        if (context.getRequest().getOptions() != null && StringUtils.hasText(context.getRequest().getOptions().getStyle())) {
            return keyValues.and(OpenLitAttributes.GEN_AI_REQUEST_IMAGE_STYLE,
                    context.getRequest().getOptions().getStyle());
        }
        return keyValues;
    }

}
