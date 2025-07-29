package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.springframework.boot.info.ProcessInfo;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;

/**
 * A {@link ResourceContributor} that contributes attributes about the Java process,
 * following the OpenTelemetry Semantic Conventions.
 * <p>
 * The following attributes are populated:
 * <ul>
 *     <li>{@code process.owner}</li>
 *     <li>{@code process.parent_pid}</li>
 *     <li>{@code process.pid}</li>
 * </ul>
 *
 * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/process/#process">Resource Process Semantic Conventions</a>
 */
@Incubating(since = "0.5.0")
public final class ProcessResourceContributor implements ResourceContributor {

    public static final AttributeKey<String> PROCESS_OWNER = AttributeKey.stringKey("process.owner");
    public static final AttributeKey<Long> PROCESS_PARENT_PID = AttributeKey.longKey("process.parent_pid");
    public static final AttributeKey<Long> PROCESS_PID = AttributeKey.longKey("process.pid");

    private final ProcessInfo processInfo = new ProcessInfo();

    @Override
    public void contribute(ResourceBuilder builder) {
        if (StringUtils.hasText(processInfo.getOwner())) {
            builder.put(PROCESS_OWNER, processInfo.getOwner());
        }
        builder.put(PROCESS_PARENT_PID, processInfo.getParentPid());
        builder.put(PROCESS_PID, processInfo.getPid());
    }

}
