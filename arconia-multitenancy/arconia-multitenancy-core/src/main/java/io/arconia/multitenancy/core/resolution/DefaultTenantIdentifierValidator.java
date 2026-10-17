package io.arconia.multitenancy.core.resolution;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;

/**
 * Default {@link TenantIdentifierValidator} implementation, accepting identifiers that
 * match a regular expression in full, up to a maximum length. By default, identifiers
 * are made up of alphanumeric characters, dashes, and underscores.
 */
@Incubating
public final class DefaultTenantIdentifierValidator implements TenantIdentifierValidator {

    private static final int DEFAULT_MAX_LENGTH = 64;

    private static final String DEFAULT_PATTERN = "[a-zA-Z0-9_-]+";

    private static final String EMPTY_MESSAGE = "The tenant identifier must not be empty";

    private static final String TOO_LONG_MESSAGE = "The tenant identifier must not be longer than %d characters";

    private static final String DEFAULT_PATTERN_MESSAGE = "The tenant identifier must contain only alphanumeric characters, dashes (-), and underscores (_)";

    private static final String CUSTOM_PATTERN_MESSAGE = "The tenant identifier must match the pattern '%s'";

    private final int maxLength;

    private final Pattern pattern;

    private final String invalidCharactersMessage;

    private DefaultTenantIdentifierValidator(int maxLength, String pattern) {
        Assert.isTrue(maxLength > 0, "maxLength must be greater than zero");
        Assert.hasText(pattern, "pattern cannot be null or empty");
        this.maxLength = maxLength;
        this.pattern = compile(pattern);
        this.invalidCharactersMessage = DEFAULT_PATTERN.equals(pattern) ? DEFAULT_PATTERN_MESSAGE
                : CUSTOM_PATTERN_MESSAGE.formatted(pattern);
    }

    /**
     * Compiles the pattern eagerly, so that a malformed expression fails when the
     * validator is created rather than on the first request.
     */
    private static Pattern compile(String pattern) {
        try {
            return Pattern.compile(pattern);
        } catch (PatternSyntaxException ex) {
            throw new IllegalArgumentException("pattern is not a valid regular expression: " + ex.getMessage(), ex);
        }
    }

    /**
     * Maximum number of characters accepted in a tenant identifier.
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Regular expression a tenant identifier must match in full.
     */
    public String getPattern() {
        return pattern.pattern();
    }

    @Override
    public void validate(String tenantIdentifier) {
        if (!StringUtils.hasText(tenantIdentifier)) {
            throw new InvalidTenantIdentifierException(EMPTY_MESSAGE);
        }
        if (tenantIdentifier.length() > maxLength) {
            throw new InvalidTenantIdentifierException(TOO_LONG_MESSAGE.formatted(maxLength));
        }
        if (!pattern.matcher(tenantIdentifier).matches()) {
            throw new InvalidTenantIdentifierException(invalidCharactersMessage);
        }
    }

    /**
     * Creates a new builder for {@link DefaultTenantIdentifierValidator}.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private int maxLength = DEFAULT_MAX_LENGTH;

        private String pattern = DEFAULT_PATTERN;

        private Builder() {}

        /**
         * Maximum number of characters accepted in a tenant identifier.
         */
        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        /**
         * Regular expression a tenant identifier must match in full.
         */
        public Builder pattern(String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * Builds the {@link DefaultTenantIdentifierValidator} instance.
         */
        public DefaultTenantIdentifierValidator build() {
            return new DefaultTenantIdentifierValidator(maxLength, pattern);
        }

    }

}
