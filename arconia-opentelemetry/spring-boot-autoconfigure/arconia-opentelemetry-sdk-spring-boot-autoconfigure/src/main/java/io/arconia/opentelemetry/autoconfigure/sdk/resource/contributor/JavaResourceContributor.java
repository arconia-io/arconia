package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.springframework.boot.info.JavaInfo;
import org.springframework.util.StringUtils;

/**
 * A {@link ResourceContributor} that contributes attributes about the Java process runtime,
 * following the OpenTelemetry Semantic Conventions.
 * <p>
 * The following attributes are populated:
 * <ul>
 *     <li>{@code process.runtime.description}</li>
 *     <li>{@code process.runtime.name}</li>
 *     <li>{@code process.runtime.version}</li>
 * </ul>
 *
 * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/process/#process-runtimes">Resource Process Runtime Semantic Conventions</a>
 */
public class JavaResourceContributor implements ResourceContributor {

    // These semantic conventions are experimental, so we define them explicitly to be able to ensure backward
    // compatibility rather than using the constants from OpenTelemetry SemConv project that may change in the future
    // without considering backward compatibility.
    public static final AttributeKey<String> PROCESS_RUNTIME_DESCRIPTION = AttributeKey.stringKey("process.runtime.description");
    public static final AttributeKey<String> PROCESS_RUNTIME_NAME = AttributeKey.stringKey("process.runtime.name");
    public static final AttributeKey<String> PROCESS_RUNTIME_VERSION = AttributeKey.stringKey("process.runtime.version");

    private final JavaInfo javaInfo = new JavaInfo();

    @Override
    public void contribute(ResourceBuilder builder) {
        if (StringUtils.hasText(javaInfo.getRuntime().getName())) {
            builder.put(PROCESS_RUNTIME_NAME, javaInfo.getRuntime().getName());
        }
        if (StringUtils.hasText(javaInfo.getRuntime().getVersion())) {
            builder.put(PROCESS_RUNTIME_VERSION, javaInfo.getRuntime().getVersion());
        }

        if (StringUtils.hasText(javaInfo.getJvm().getVendor()) && StringUtils.hasText(javaInfo.getJvm().getName()) && StringUtils.hasText(javaInfo.getJvm().getVersion())) {
            builder.put(PROCESS_RUNTIME_DESCRIPTION, "%s %s %s".formatted(javaInfo.getJvm().getVendor(),
                    javaInfo.getJvm().getName(), javaInfo.getJvm().getVersion()));
        }
    }

}
