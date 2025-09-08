package io.arconia.openinference.observation.instrumentation;

import com.arize.semconv.trace.SemanticResourceAttributes;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import io.arconia.opentelemetry.autoconfigure.resource.contributor.ResourceContributor;

/**
 * A {@link ResourceContributor} that contribute resource attributes based on the OpenInference specification.
 */
public class OpenInferenceResourceContributor implements ResourceContributor {

    @Override
    public void contribute(ResourceBuilder builder) {
        String serviceName = builder.build().getAttribute(AttributeKey.stringKey("service.name"));
        builder.put(SemanticResourceAttributes.SEMRESATTRS_PROJECT_NAME, serviceName);
    }

}
