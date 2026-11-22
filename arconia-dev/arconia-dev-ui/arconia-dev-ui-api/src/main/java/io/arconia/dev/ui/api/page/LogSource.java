package io.arconia.dev.ui.api.page;

import java.util.function.Consumer;

import io.arconia.core.annotations.Incubating;

/**
 * A log that the Dev UI can follow, such as the output of a container.
 */
@Incubating
@FunctionalInterface
public interface LogSource {

    /**
     * Follows the log, passing each line to the given consumer as it is written, and returns
     * when the log ends.
     * <p>
     * The method is called on a thread of its own each time someone opens the log in the Dev
     * UI, and that thread is interrupted when they stop watching. The implementation must then
     * release what it holds and return, or let the {@link InterruptedException} through:
     * otherwise, the thread is left running for as long as the log goes on.
     * <p>
     * The consumer can be called from any thread, one line at a time, and it can block for a
     * while when lines arrive faster than they are shown. Lines passed to it after this method
     * has returned are ignored.
     *
     * @param lines the consumer of the lines of the log, without their line ending
     * @throws Exception if the log cannot be followed, which the Dev UI reports to the viewer
     */
    void follow(Consumer<String> lines) throws Exception;

}
