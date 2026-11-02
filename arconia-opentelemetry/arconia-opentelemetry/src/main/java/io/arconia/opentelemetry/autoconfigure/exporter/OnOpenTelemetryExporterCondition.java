package io.arconia.opentelemetry.autoconfigure.exporter;

import java.lang.annotation.Annotation;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.core.annotations.Internal;

/**
 * Determines if a certain exporter type is enabled for an OpenTelemetry signal.
 * <p>
 * The exporter type configured for the signal takes precedence over the common one,
 * which defaults to OTLP. Setting the common exporter type to an empty value disables
 * the exporters of the signals that don't configure their own type.
 */
@Internal
public abstract class OnOpenTelemetryExporterCondition extends SpringBootCondition {

    private static final String COMMON_EXPORTER_TYPE = OpenTelemetryExporterProperties.CONFIG_PREFIX + ".type";

    private final Class<? extends Annotation> annotationType;

    private final String signalExporterType;

    /**
     * Creates a condition for the signal whose exporter properties have the given prefix,
     * such as {@code arconia.otel.logs.exporter}. The annotation holds the requested exporter type
     * in its {@code value} attribute.
     */
    protected OnOpenTelemetryExporterCondition(Class<? extends Annotation> annotationType, String signalExporterPrefix) {
        Assert.notNull(annotationType, "annotationType cannot be null");
        Assert.hasText(signalExporterPrefix, "signalExporterPrefix cannot be null or empty");
        this.annotationType = annotationType;
        this.signalExporterType = signalExporterPrefix + ".type";
    }

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConditionMessage.Builder message = ConditionMessage.forCondition(annotationType);

        MergedAnnotation<? extends Annotation> annotation = metadata.getAnnotations().get(annotationType);
        String requestedExporterType = annotation.isPresent() ? annotation.getString(MergedAnnotation.VALUE) : null;
        if (!StringUtils.hasText(requestedExporterType)) {
            return ConditionOutcome.noMatch(message.because("a valid exporter type is not specified"));
        }

        // Bound like the configuration properties so that invalid values are reported with the property name.
        Binder binder = Binder.get(context.getEnvironment());
        String property = signalExporterType;
        ExporterType exporterType = binder.bind(signalExporterType, ExporterType.class).orElse(null);
        if (exporterType == null) {
            property = COMMON_EXPORTER_TYPE;
            exporterType = binder.bind(COMMON_EXPORTER_TYPE, ExporterType.class).orElse(null);
        }
        if (exporterType == null) {
            if (context.getEnvironment().containsProperty(COMMON_EXPORTER_TYPE)) {
                return ConditionOutcome.noMatch(message.because(COMMON_EXPORTER_TYPE + " is empty"));
            }
            exporterType = ExporterType.OTLP;
        }

        if (exporterType.name().equalsIgnoreCase(requestedExporterType.strip())) {
            return ConditionOutcome.match(message.because(property + " is set to " + exporterType));
        }
        return ConditionOutcome.noMatch(message.because(property + " is set to " + exporterType + ", but requested " + requestedExporterType));
    }

}
