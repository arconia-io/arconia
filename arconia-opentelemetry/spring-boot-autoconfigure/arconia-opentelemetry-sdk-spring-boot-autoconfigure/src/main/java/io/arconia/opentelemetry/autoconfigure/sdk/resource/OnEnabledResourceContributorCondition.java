/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.arconia.opentelemetry.autoconfigure.sdk.resource;

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
 * @see ConditionalOnEnabledResourceContributor
 */
class OnEnabledResourceContributorCondition extends SpringBootCondition {

    private static final String CONTRIBUTOR_PROPERTY = OpenTelemetryResourceProperties.CONFIG_PREFIX + ".%s.enabled";

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(ConditionalOnEnabledResourceContributor.class.getName());
        String contributorName = attributes != null ? (String) attributes.get("value") : null;
        boolean matchIfMissing = attributes != null && (boolean) attributes.get("matchIfMissing");

        if (StringUtils.hasLength(contributorName)) {
            Boolean contributorEnabled = context.getEnvironment()
                    .getProperty(CONTRIBUTOR_PROPERTY.formatted(contributorName), Boolean.class);
            if (contributorEnabled != null) {
                return new ConditionOutcome(contributorEnabled,
                        ConditionMessage.forCondition(ConditionalOnEnabledResourceContributor.class)
                                .because(CONTRIBUTOR_PROPERTY.formatted(contributorName) + " is " + contributorEnabled));
            }
            if (matchIfMissing) {
                return ConditionOutcome.match(ConditionMessage.forCondition(ConditionalOnEnabledResourceContributor.class)
                        .because("resource contributor is enabled by default"));
            } else {
                return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnEnabledResourceContributor.class)
                        .because("resource contributor is disabled by default"));
            }
        }

        return ConditionOutcome.noMatch(ConditionMessage.forCondition(ConditionalOnEnabledResourceContributor.class)
                .because("resource contributor is disabled by default"));
    }

}
