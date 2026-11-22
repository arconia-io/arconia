package io.arconia.dev.services.core.devui;

import java.util.function.Consumer;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.OutputFrame.OutputType;

/**
 * Follows the log of a container through the container runtime.
 */
final class ContainerLogs {

    /**
     * How many of the lines already written are shown before following the log. The log of a
     * container running for a long time can be very large.
     */
    private static final int TAIL_LINES = 1000;

    private ContainerLogs() {}

    /**
     * Passes the last lines of the log of the given container to the consumer, and then each
     * new line as it is written, until the container stops or the calling thread is
     * interrupted.
     */
    static void follow(String containerId, Consumer<String> lines) throws Exception {
        try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            Consumer<OutputFrame> frames = frame -> lines.accept(frame.getUtf8StringWithoutLineEnding());
            callback.addConsumer(OutputType.STDOUT, frames);
            callback.addConsumer(OutputType.STDERR, frames);
            // The client of the container runtime is shared and managed by Testcontainers, so
            // only the callback of this log is closed.
            DockerClientFactory.lazyClient()
                    .logContainerCmd(containerId)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withFollowStream(true)
                    .withTail(TAIL_LINES)
                    .exec(callback);
            callback.awaitCompletion();
        }
    }

}
