package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import io.opentelemetry.sdk.resources.ResourceBuilder;
import io.opentelemetry.semconv.ServiceAttributes;

import org.springframework.boot.info.BuildProperties;
import org.springframework.util.StringUtils;

/**
 * A {@link ResourceContributor} that contributes build information.
 * <p>
 * The following attributes are populated:
 * <ul>
 *     <li>{@code service.version}</li>
 * </ul>
 */
public class BuildResourceContributor implements ResourceContributor {

    private final BuildProperties buildProperties;

    public BuildResourceContributor(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    public void contribute(ResourceBuilder builder) {
        if (StringUtils.hasText(buildProperties.getVersion())) {
            builder.put(ServiceAttributes.SERVICE_VERSION, buildProperties.getVersion());
        }
    }

}
