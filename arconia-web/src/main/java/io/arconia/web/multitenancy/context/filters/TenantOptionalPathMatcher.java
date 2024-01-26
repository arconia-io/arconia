package io.arconia.web.multitenancy.context.filters;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Matches HTTP requests paths for which a tenant context is optional.
 *
 * @author Thomas Vitale
 */
public class TenantOptionalPathMatcher {

    private static final Logger log = LoggerFactory.getLogger(TenantOptionalPathMatcher.class);

    private final List<PathPattern> optionalPathPatterns;

    public TenantOptionalPathMatcher(List<String> optionalPathPatterns) {
        Assert.notNull(optionalPathPatterns, "optionalPathPatterns cannot be null");
        this.optionalPathPatterns = optionalPathPatterns.stream().map(this::parse).toList();
    }

    public boolean matches(HttpServletRequest httpServletRequest) {
        Assert.notNull(httpServletRequest, "httpServletRequest cannot be null");
        var requestUri = httpServletRequest.getRequestURI();
        var pathContainer = PathContainer.parsePath(requestUri);
        var matchesOptionalPaths = optionalPathPatterns.stream()
            .anyMatch(pathPattern -> pathPattern.matches(pathContainer));
        if (matchesOptionalPaths) {
            log.debug("Request '" + requestUri + "' matches one of the paths for which a tenant is optional");
        }
        return matchesOptionalPaths;
    }

    private PathPattern parse(String pattern) {
        var parser = PathPatternParser.defaultInstance;
        pattern = parser.initFullPathPattern(pattern);
        return parser.parse(pattern);
    }

}
