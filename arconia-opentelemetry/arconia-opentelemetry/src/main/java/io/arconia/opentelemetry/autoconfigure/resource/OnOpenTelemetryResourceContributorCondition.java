package io.arconia.opentelemetry.autoconfigure.resource;

import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Determines if a resource contributor is enabled.
 *
 * @see ConditionalOnOpenTelemetryResourceContributor
 */
class OnOpenTelemetryResourceContributorCondition extends SpringBootCondition {

    private static final String CONTRIBUTOR_PROPERTY = OpenTelemetryResourceProperties.CONFIG_PREFIX + ".contributors.%s.enabled";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnOpenTelemetryResourceContributor.class.getName());
        String contributorName = attributes != null ? (String) attributes.get("value") : null;
        boolean matchIfMissing = attributes != null && (boolean) attributes.get("matchIfMissing");

        if (StringUtils.hasLength(contributorName)) {
            Boolean contributorEnabled = context.getEnvironment()
                    .getProperty(CONTRIBUTOR_PROPERTY.formatted(contributorName), Boolean.class);
            if (contributorEnabled != null) {
                return new ConditionOutcome(contributorEnabled,
                        ConditionMessage.forCondition(ConditionalOnOpenTelemetryResourceContributor.class)
                                .because(CONTRIBUTOR_PROPERTY.formatted(contributorName) + " is " + contributorEnabled));
            }
            if (matchIfMissing) {
                return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnOpenTelemetryResourceContributor.class)
                        .because("resource contributor is enabled by default"));
            } else {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryResourceContributor.class)
                        .because("resource contributor is disabled by default"));
            }
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnOpenTelemetryResourceContributor.class)
                .because("no resource contributor name provided"));
    }

}
