package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.info.GitProperties;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;

/**
 * A {@link ResourceContributor} that contributes attributes about the version control
 * revision the application was built from, following the OpenTelemetry Semantic Conventions.
 * <p>
 * The following attributes are populated:
 * <ul>
 *     <li>{@code vcs.ref.head.name}</li>
 *     <li>{@code vcs.ref.head.revision}</li>
 *     <li>{@code vcs.ref.head.type}</li>
 *     <li>{@code vcs.repository.name}</li>
 *     <li>{@code vcs.repository.url.full}</li>
 * </ul>
 *
 * @link <a href="https://opentelemetry.io/docs/specs/semconv/registry/attributes/vcs/">VCS Semantic Conventions</a>
 */
@Incubating
public final class GitResourceContributor implements ResourceContributor {

    // These semantic conventions are experimental, so we define them explicitly to be able to ensure backward
    // compatibility rather than using the constants from OpenTelemetry SemConv project that may change in the future
    // without considering backward compatibility.
    public static final AttributeKey<String> VCS_REF_HEAD_NAME = AttributeKey.stringKey("vcs.ref.head.name");
    public static final AttributeKey<String> VCS_REF_HEAD_REVISION = AttributeKey.stringKey("vcs.ref.head.revision");
    public static final AttributeKey<String> VCS_REF_HEAD_TYPE = AttributeKey.stringKey("vcs.ref.head.type");
    public static final AttributeKey<String> VCS_REPOSITORY_NAME = AttributeKey.stringKey("vcs.repository.name");
    public static final AttributeKey<String> VCS_REPOSITORY_URL_FULL = AttributeKey.stringKey("vcs.repository.url.full");

    private static final String REF_TYPE_BRANCH = "branch";

    private static final String REMOTE_ORIGIN_URL = "remote.origin.url";

    /**
     * Matches the scp-like syntax of SSH remotes, such as {@code git@github.com:org/repo.git}.
     * The host has at least two characters, so that Windows paths like {@code C:/repo} don't match.
     */
    private static final Pattern SCP_LIKE_URL = Pattern.compile("^(?:[^@/]+@)?([^:/]{2,}):(?!//)(.+)$");

    private final GitProperties gitProperties;

    public GitResourceContributor(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Override
    public void contribute(ResourceBuilder builder) {
        if (StringUtils.hasText(gitProperties.getCommitId())) {
            builder.put(VCS_REF_HEAD_REVISION, gitProperties.getCommitId());
        }
        if (StringUtils.hasText(gitProperties.getBranch())) {
            builder.put(VCS_REF_HEAD_NAME, gitProperties.getBranch());
            builder.put(VCS_REF_HEAD_TYPE, REF_TYPE_BRANCH);
        }
        String remoteUrl = gitProperties.get(REMOTE_ORIGIN_URL);
        String repositoryUrl = StringUtils.hasText(remoteUrl) ? toRepositoryUrl(remoteUrl) : null;
        if (repositoryUrl != null) {
            builder.put(VCS_REPOSITORY_URL_FULL, repositoryUrl);
            builder.put(VCS_REPOSITORY_NAME, repositoryUrl.substring(repositoryUrl.lastIndexOf('/') + 1));
        }
    }

    /**
     * Converts a Git remote URL into the canonical HTTP(S) URL of the repository, without credentials and
     * without the {@code .git} extension, as the conventions require. SSH remotes are converted to HTTPS URLs
     * on the same host. Returns {@code null} for remotes that can't be converted.
     */
    static @Nullable String toRepositoryUrl(String remoteUrl) {
        String url = remoteUrl.trim();
        if (url.startsWith("https://") || url.startsWith("http://") || url.startsWith("ssh://")) {
            try {
                URI uri = URI.create(url);
                if (uri.getHost() == null || uri.getPath() == null) {
                    return null;
                }
                if ("ssh".equals(uri.getScheme())) {
                    return canonical("https://" + uri.getHost(), uri.getPath());
                }
                String authority = (uri.getPort() != -1) ? uri.getHost() + ":" + uri.getPort() : uri.getHost();
                return canonical(uri.getScheme() + "://" + authority, uri.getPath());
            }
            catch (IllegalArgumentException ex) {
                return null;
            }
        }
        Matcher scpLike = SCP_LIKE_URL.matcher(url);
        if (scpLike.matches()) {
            return canonical("https://" + scpLike.group(1), "/" + scpLike.group(2));
        }
        return null;
    }

    private static @Nullable String canonical(String origin, String path) {
        String canonicalPath = path;
        while (canonicalPath.endsWith("/")) {
            canonicalPath = canonicalPath.substring(0, canonicalPath.length() - 1);
        }
        if (canonicalPath.endsWith(".git")) {
            canonicalPath = canonicalPath.substring(0, canonicalPath.length() - ".git".length());
        }
        if (!StringUtils.hasText(canonicalPath) || "/".equals(canonicalPath)) {
            return null;
        }
        return origin + (canonicalPath.startsWith("/") ? canonicalPath : "/" + canonicalPath);
    }

}
