package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.springframework.boot.info.OsInfo;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Internal;

/**
 * A {@link ResourceContributor} that contributes attributes about the operating system,
 * following the OpenTelemetry Semantic Conventions.
 * <p>
 * The following attributes are populated:
 * <ul>
 *     <li>{@code os.description}</li>
 *     <li>{@code os.name}</li>
 *     <li>{@code os.type}</li>
 *     <li>{@code os.version}</li>
 * </ul>
 *
 * @link <a href="https://opentelemetry.io/docs/specs/semconv/resource/os">Resource OS Semantic Conventions</a>
 */
@Internal
public class OsResourceContributor implements ResourceContributor {

    // These semantic conventions are experimental, so we define them explicitly to be able to ensure backward
    // compatibility rather than using the constants from OpenTelemetry SemConv project that may change in the future
    // without considering backward compatibility.
    public static final AttributeKey<String> OS_DESCRIPTION = AttributeKey.stringKey("os.description");
    public static final AttributeKey<String> OS_NAME = AttributeKey.stringKey("os.name");
    public static final AttributeKey<String> OS_TYPE = AttributeKey.stringKey("os.type");
    public static final AttributeKey<String> OS_VERSION = AttributeKey.stringKey("os.version");

    private final OsInfo osInfo = new OsInfo();

    @Override
    public void contribute(ResourceBuilder builder) {
        if (StringUtils.hasText(osInfo.getName())) {
            builder.put(OS_NAME, osInfo.getName());
            builder.put(OS_TYPE, computeOsType(osInfo.getName()));
        }
        if (StringUtils.hasText(osInfo.getVersion())) {
            builder.put(OS_VERSION, osInfo.getVersion());
        }

        if (StringUtils.hasText(osInfo.getName()) && StringUtils.hasText(osInfo.getVersion()) && StringUtils.hasText(osInfo.getArch())) {
            builder.put(OS_DESCRIPTION, "%s [Version: %s, Architecture: %s]".formatted(osInfo.getName(), osInfo.getVersion(), osInfo.getArch()));
        } else if (StringUtils.hasText(osInfo.getName()) && StringUtils.hasText(osInfo.getVersion())) {
            builder.put(OS_DESCRIPTION, "%s [Version: %s]".formatted(osInfo.getName(), osInfo.getVersion()));
        } else if (StringUtils.hasText(osInfo.getName())) {
            builder.put(OS_DESCRIPTION, osInfo.getName());
        }
    }

    private String computeOsType(String osName) {
        String os = osName.toLowerCase().trim();
        if (os.contains(OsType.AIX.getMatcher())) {
            return OsType.AIX.getValue();
        } else if (os.contains(OsType.DARWIN.getMatcher())) {
            return OsType.DARWIN.getValue();
        } else if (os.contains(OsType.DRAGONFLYBSD.getMatcher())) {
            return OsType.DRAGONFLYBSD.getValue();
        } else if (os.contains(OsType.FREEBSD.getMatcher())) {
            return OsType.FREEBSD.getValue();
        } else if (os.contains(OsType.HPUX.getMatcher())) {
            return OsType.HPUX.getValue();
        } else if (os.contains(OsType.LINUX.getMatcher())) {
            return OsType.LINUX.getValue();
        } else if (os.contains(OsType.NETBSD.getMatcher())) {
            return OsType.NETBSD.getValue();
        } else if (os.contains(OsType.OPENBSD.getMatcher())) {
            return OsType.OPENBSD.getValue();
        } else if (os.contains(OsType.SOLARIS.getMatcher())) {
            return OsType.SOLARIS.getValue();
        } else if (os.contains(OsType.WINDOWS.getMatcher())) {
            return OsType.WINDOWS.getValue();
        } else if (os.contains(OsType.ZOS.getMatcher())) {
            return OsType.ZOS.getValue();
        }
        return osName;
    }

    enum OsType {
        AIX("aix", "aix"),
        DARWIN("darwin", "mac"),
        DRAGONFLYBSD("dragonflybsd", "dragonfly"),
        FREEBSD("freebsd", "freebsd"),
        HPUX("hpux", "hp-ux"),
        LINUX("linux", "linux"),
        NETBSD("netbsd", "netbsd"),
        OPENBSD("openbsd", "openbsd"),
        SOLARIS("solaris", "solaris"),
        WINDOWS("windows", "windows"),
        ZOS("zos", "z/os");

        private final String value;
        private final String matcher;

        OsType(String value, String matcher) {
            this.value = value;
            this.matcher = matcher;
        }

        public String getValue() {
            return value;
        }

        public String getMatcher() {
            return matcher;
        }
    }

}
