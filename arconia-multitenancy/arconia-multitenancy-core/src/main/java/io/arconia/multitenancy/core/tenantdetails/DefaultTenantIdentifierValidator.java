package io.arconia.multitenancy.core.tenantdetails;

import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import io.arconia.core.support.Incubating;
import io.arconia.multitenancy.core.exceptions.TenantVerificationException;

/**
 * Default {@link TenantIdentifierValidator} implementation, accepting identifiers made up
 * of alphanumeric characters, dashes, and underscores, up to a maximum length.
 */
@Incubating
public final class DefaultTenantIdentifierValidator implements TenantIdentifierValidator {

    private static final int DEFAULT_MAX_LENGTH = 64;

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z0-9_-]+");

    private static final String EMPTY_MESSAGE = "The tenant identifier must not be empty";

    private static final String TOO_LONG_MESSAGE = "The tenant identifier must not be longer than %d characters";

    private static final String INVALID_CHARACTERS_MESSAGE = "The tenant identifier must contain only alphanumeric characters, dashes (-), and underscores (_)";

    private final int maxLength;

    private DefaultTenantIdentifierValidator(int maxLength) {
        Assert.isTrue(maxLength > 0, "maxLength must be greater than zero");
        this.maxLength = maxLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public void validate(String tenantIdentifier) {
        if (!StringUtils.hasText(tenantIdentifier)) {
            throw new TenantVerificationException(EMPTY_MESSAGE);
        }
        if (tenantIdentifier.length() > maxLength) {
            throw new TenantVerificationException(TOO_LONG_MESSAGE.formatted(maxLength));
        }
        if (!IDENTIFIER_PATTERN.matcher(tenantIdentifier).matches()) {
            throw new TenantVerificationException(INVALID_CHARACTERS_MESSAGE);
        }
    }

    /**
     * Creates a new builder for {@link DefaultTenantIdentifierValidator}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DefaultTenantIdentifierValidator}.
     */
    public static final class Builder {

        private int maxLength = DEFAULT_MAX_LENGTH;

        private Builder() {}

        /**
         * Maximum number of characters accepted in a tenant identifier.
         */
        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        /**
         * Builds the {@link DefaultTenantIdentifierValidator} instance.
         */
        public DefaultTenantIdentifierValidator build() {
            return new DefaultTenantIdentifierValidator(maxLength);
        }

    }

}
