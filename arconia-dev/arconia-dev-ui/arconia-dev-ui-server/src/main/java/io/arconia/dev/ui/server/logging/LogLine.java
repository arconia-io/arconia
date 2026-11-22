package io.arconia.dev.ui.server.logging;

import org.jspecify.annotations.Nullable;

import io.arconia.core.annotations.Internal;

/**
 * A line of the logs of the application, as the console of the Dev UI shows it.
 *
 * @param time when the line was logged, in milliseconds since the epoch
 * @param level the level of the line (e.g. {@code INFO}), or {@code null} for a marker
 * @param logger the name of the logger, or {@code null} for a marker
 * @param message the message of the line
 * @param stackTrace the stack trace of the exception logged with the line, if any
 * @param marker whether the line is not from the application, but tells something about the
 * logs themselves, such as the application having been restarted
 */
@Internal
public record LogLine(long time, @Nullable String level, @Nullable String logger, String message, @Nullable String stackTrace, boolean marker) {

    static LogLine marker(String message) {
        return new LogLine(System.currentTimeMillis(), null, null, message, null, true);
    }

}
