package io.arconia.multitenancy.web.context.filters;

import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import io.arconia.core.support.Incubating;

/**
 * Matches HTTP requests paths for which a tenant context is not attached.
 */
@Incubating(since = "0.2.0")
public class TenantContextIgnorePathMatcher {

    private static final Logger logger = LoggerFactory.getLogger(TenantContextIgnorePathMatcher.class);

    private final List<PathPattern> ignorePathPatterns;

    public TenantContextIgnorePathMatcher(Set<String> ignorePathPatterns) {
        Assert.notNull(ignorePathPatterns, "ignorePathPatterns cannot be null");
        this.ignorePathPatterns = ignorePathPatterns.stream().map(this::parse).toList();
    }

    public boolean matches(HttpServletRequest httpServletRequest) {
        Assert.notNull(httpServletRequest, "httpServletRequest cannot be null");
        var requestUri = httpServletRequest.getRequestURI();
        var pathContainer = PathContainer.parsePath(requestUri);
        var matchesIgnorePaths = ignorePathPatterns.stream()
            .anyMatch(pathPattern -> pathPattern.matches(pathContainer));
        if (matchesIgnorePaths) {
            logger.debug(
                    "Request '" + requestUri + "' matches one of the paths to ignore when attaching a tenant context");
        }
        return matchesIgnorePaths;
    }

    private PathPattern parse(String pattern) {
        var parser = PathPatternParser.defaultInstance;
        pattern = parser.initFullPathPattern(pattern);
        return parser.parse(pattern);
    }

}
