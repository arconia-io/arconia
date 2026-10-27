package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import java.time.Instant;

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
 *     <li>{@code process.creation.time}</li>
 *     <li>{@code process.owner}</li>
 *     <li>{@code process.parent_pid}</li>
 *     <li>{@code process.pid}</li>
 *     <li>{@code process.working_directory}</li>
 * </ul>
 *
 * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/process/#process">Resource Process Semantic Conventions</a>
 */
@Incubating
public final class ProcessResourceContributor implements ResourceContributor {

    // These semantic conventions are experimental, so we define them explicitly to be able to ensure backward
    // compatibility rather than using the constants from OpenTelemetry SemConv project that may change in the future
    // without considering backward compatibility.
    public static final AttributeKey<String> PROCESS_CREATION_TIME = AttributeKey.stringKey("process.creation.time");
    public static final AttributeKey<String> PROCESS_OWNER = AttributeKey.stringKey("process.owner");
    public static final AttributeKey<Long> PROCESS_PARENT_PID = AttributeKey.longKey("process.parent_pid");
    public static final AttributeKey<Long> PROCESS_PID = AttributeKey.longKey("process.pid");
    public static final AttributeKey<String> PROCESS_WORKING_DIRECTORY = AttributeKey.stringKey("process.working_directory");

    private final ProcessInfo processInfo = new ProcessInfo();

    @Override
    public void contribute(ResourceBuilder builder) {
        Instant startTime = processInfo.getStartTime();
        if (startTime != null) {
            builder.put(PROCESS_CREATION_TIME, startTime.toString());
        }
        if (StringUtils.hasText(processInfo.getOwner())) {
            builder.put(PROCESS_OWNER, processInfo.getOwner());
        }
        // Spring Boot reports -1 when the process has no parent.
        if (processInfo.getParentPid() >= 0) {
            builder.put(PROCESS_PARENT_PID, processInfo.getParentPid());
        }
        builder.put(PROCESS_PID, processInfo.getPid());
        if (StringUtils.hasText(processInfo.getWorkingDirectory())) {
            builder.put(PROCESS_WORKING_DIRECTORY, processInfo.getWorkingDirectory());
        }
    }

}
