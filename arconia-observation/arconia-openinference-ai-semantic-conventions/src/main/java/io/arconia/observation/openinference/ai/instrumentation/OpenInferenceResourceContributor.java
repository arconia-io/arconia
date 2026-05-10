package io.arconia.observation.openinference.ai.instrumentation;

import com.arize.semconv.trace.SemanticResourceAttributes;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

import io.arconia.opentelemetry.autoconfigure.resource.contributor.ResourceContributor;

/**
 * A {@link ResourceContributor} that contributes resource attributes based on the OpenInference specification.
 */
public class OpenInferenceResourceContributor implements ResourceContributor {

    @Nullable
    private final String projectName;

    public OpenInferenceResourceContributor() {
        this(null);
    }

    public OpenInferenceResourceContributor(@Nullable String projectName) {
        this.projectName = projectName;
    }

    @Override
    public void contribute(ResourceBuilder builder) {
        String resourceProjectName = StringUtils.hasText(projectName) ? projectName :
            builder.build().getAttribute(AttributeKey.stringKey("service.name"));
        if (StringUtils.hasText(resourceProjectName)) {
            builder.put(SemanticResourceAttributes.SEMRESATTRS_PROJECT_NAME, resourceProjectName);
        }
    }

}
