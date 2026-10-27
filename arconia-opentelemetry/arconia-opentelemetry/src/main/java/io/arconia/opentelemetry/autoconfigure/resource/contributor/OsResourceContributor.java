package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.info.OsInfo;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;

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
@Incubating
public final class OsResourceContributor implements ResourceContributor {

    // These semantic conventions are experimental, so we define them explicitly to be able to ensure backward
    // compatibility rather than using the constants from OpenTelemetry SemConv project that may change in the future
    // without considering backward compatibility.
    public static final AttributeKey<String> OS_DESCRIPTION = AttributeKey.stringKey("os.description");
    public static final AttributeKey<String> OS_NAME = AttributeKey.stringKey("os.name");
    public static final AttributeKey<String> OS_TYPE = AttributeKey.stringKey("os.type");
    public static final AttributeKey<String> OS_VERSION = AttributeKey.stringKey("os.version");

    /**
     * The JVM reports Solaris as "SunOS".
     */
    private static final String SUNOS_MATCHER = "sunos";

    private static final String PRETTY_NAME_PREFIX = "PRETTY_NAME=";

    private final OsInfo osInfo = new OsInfo();

    private final List<Path> osReleasePaths = List.of(Path.of("/etc/os-release"), Path.of("/usr/lib/os-release"));

    @Override
    public void contribute(ResourceBuilder builder) {
        if (StringUtils.hasText(osInfo.getName())) {
            builder.put(OS_NAME, osInfo.getName());
            builder.put(OS_TYPE, computeOsType(osInfo.getName()));
        }
        if (StringUtils.hasText(osInfo.getVersion())) {
            builder.put(OS_VERSION, osInfo.getVersion());
        }

        String osDescription = computeOsDescription();
        if (StringUtils.hasText(osDescription)) {
            builder.put(OS_DESCRIPTION, osDescription);
        }
    }

    /**
     * Computes a human-readable description of the operating system, like the one reported by {@code lsb_release}
     * or {@code ver}, as the conventions suggest. On Linux, it's the {@code PRETTY_NAME} of the os-release file,
     * such as "Ubuntu 24.04.1 LTS". Otherwise, it's made of the operating system name and version.
     */
    private @Nullable String computeOsDescription() {
        if (!StringUtils.hasText(osInfo.getName())) {
            return null;
        }
        if (OsType.LINUX.getValue().equals(computeOsType(osInfo.getName()))) {
            String prettyName = readOsReleasePrettyName();
            if (StringUtils.hasText(prettyName)) {
                return prettyName;
            }
        }
        if (StringUtils.hasText(osInfo.getVersion())) {
            return "%s %s".formatted(osInfo.getName(), osInfo.getVersion());
        }
        return osInfo.getName();
    }

    /**
     * Reads the {@code PRETTY_NAME} from the first os-release file that exists.
     *
     * @see <a href="https://www.freedesktop.org/software/systemd/man/latest/os-release.html">os-release</a>
     */
    private @Nullable String readOsReleasePrettyName() {
        for (Path osReleasePath : osReleasePaths) {
            if (Files.isRegularFile(osReleasePath)) {
                try (Stream<String> lines = Files.lines(osReleasePath)) {
                    return lines.filter(line -> line.startsWith(PRETTY_NAME_PREFIX))
                            .map(line -> unquote(line.substring(PRETTY_NAME_PREFIX.length()).trim()))
                            .findFirst()
                            .orElse(null);
                }
                catch (IOException | UncheckedIOException ex) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"") || value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String computeOsType(String osName) {
        String os = osName.toLowerCase(Locale.ROOT).trim();
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
        } else if (os.contains(OsType.SOLARIS.getMatcher()) || os.contains(SUNOS_MATCHER)) {
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
