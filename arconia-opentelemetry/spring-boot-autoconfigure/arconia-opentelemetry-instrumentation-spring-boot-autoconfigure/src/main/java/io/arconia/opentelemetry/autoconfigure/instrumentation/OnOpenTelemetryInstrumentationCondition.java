package io.arconia.opentelemetry.autoconfigure.instrumentation;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Determines if OpenTelemetry Instrumentation is enabled.
 *
 * @see ConditionalOnOpenTelemetryInstrumentation
 */
public class OnOpenTelemetryInstrumentationCondition extends SpringBootCondition {

    private static final String GLOBAL_PROPERTY = "arconia.otel.instrumentation.enabled";
    private static final String INSTRUMENTATION_PROPERTY = "arconia.otel.instrumentation.%s.enabled";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String instrumentationName = getInstrumentationName(metadata);
        if (StringUtils.hasLength(instrumentationName)) {
            Boolean instrumentationEnabled = context.getEnvironment()
                    .getProperty(INSTRUMENTATION_PROPERTY.formatted(instrumentationName), Boolean.class);
            if (instrumentationEnabled != null) {
                return new ConditionOutcome(instrumentationEnabled,
                        ConditionMessage.forCondition(ConditionalOnOpenTelemetryInstrumentation.class)
                                .because(INSTRUMENTATION_PROPERTY.formatted(instrumentationName) + " is " + instrumentationEnabled));
            }
        }
        Boolean globalInstrumentationEnabled = context.getEnvironment().getProperty(GLOBAL_PROPERTY, Boolean.class);
        if (globalInstrumentationEnabled != null) {
            return new ConditionOutcome(globalInstrumentationEnabled,
                    ConditionMessage.forCondition(ConditionalOnOpenTelemetryInstrumentation.class)
                            .because(GLOBAL_PROPERTY + " is " + globalInstrumentationEnabled));
        }
        return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryInstrumentation.class)
                .because("instrumentation is enabled by default"));
    }

    private static String getInstrumentationName(AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryInstrumentation.class.getName());
        if (attributes == null) {
            return null;
        }
        return (String) attributes.get("value");
    }

}
