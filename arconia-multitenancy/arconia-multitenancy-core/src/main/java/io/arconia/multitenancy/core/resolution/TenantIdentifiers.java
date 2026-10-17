package io.arconia.multitenancy.core.resolution;

import io.arconia.core.support.Internal;

/**
 * Helpers for handling tenant identifiers that have not been validated yet.
 */
@Internal
public final class TenantIdentifiers {

    private static final int LOG_MAX_LENGTH = 64;

    private static final char REPLACEMENT_CHAR = '�';

    private TenantIdentifiers() {}

    /**
     * Returns a form of the given value that is safe to log: control characters are
     * replaced, and the length is capped, so that an attacker-controlled identifier can
     * neither inject log lines nor flood the log.
     */
    public static String sanitizeForLogging(String value) {
        var length = Math.min(value.length(), LOG_MAX_LENGTH);
        var builder = new StringBuilder(length + 1);
        for (int i = 0; i < length; i++) {
            var c = value.charAt(i);
            builder.append(Character.isISOControl(c) ? REPLACEMENT_CHAR : c);
        }
        if (value.length() > LOG_MAX_LENGTH) {
            builder.append('…');
        }
        return builder.toString();
    }

}
