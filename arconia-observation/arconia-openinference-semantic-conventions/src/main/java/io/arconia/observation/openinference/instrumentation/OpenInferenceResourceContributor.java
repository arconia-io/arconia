package io.arconia.observation.openinference.instrumentation;

import com.arize.semconv.trace.SemanticResourceAttributes;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.springframework.util.StringUtils;

import io.arconia.opentelemetry.autoconfigure.resource.contributor.ResourceContributor;

/**
 * A {@link ResourceContributor} that contributes resource attributes based on the OpenInference specification.
 */
public class OpenInferenceResourceContributor implements ResourceContributor {

    @Override
    public void contribute(ResourceBuilder builder) {
        String serviceName = builder.build().getAttribute(AttributeKey.stringKey("service.name"));
        if (StringUtils.hasText(serviceName)) {
            builder.put(SemanticResourceAttributes.SEMRESATTRS_PROJECT_NAME, serviceName);
        }
    }

}
