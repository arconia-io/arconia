package io.arconia.dev.ui.server.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import io.arconia.core.annotations.Internal;

/**
 * The most recent lines of the logs of the application, kept for the console of the Dev UI.
 * <p>
 * There is one instance for the JVM. Spring Boot DevTools restarts the application by loading
 * its classes again, but not the ones of its libraries, so the lines are still there after a
 * restart and the console shows the run that ended followed by the one that began.
 * <p>
 * Adding a line never waits: the logging of the application must not slow down because
 * someone is looking at the console, or has stopped reading it.
 */
@Internal
public final class ApplicationLogs {

    static final int CAPACITY = 2000;

    private static final int FOLLOWER_CAPACITY = CAPACITY + 8000;

    private static final ApplicationLogs INSTANCE = new ApplicationLogs();

    private final Deque<LogLine> lines = new ArrayDeque<>();

    private final List<BlockingQueue<LogLine>> followers = new ArrayList<>();

    private volatile boolean capturing;

    ApplicationLogs() {}

    public static ApplicationLogs get() {
        return INSTANCE;
    }

    /**
     * Whether the logs of the application are being captured, which takes a logging system
     * the Dev UI knows how to listen to.
     */
    public boolean isCapturing() {
        return this.capturing;
    }

    /**
     * Called each time the capture starts, which is once for each run of the application.
     * From the second time on, the lines of the earlier run are followed by a marker.
     */
    synchronized void startCapturing() {
        this.capturing = true;
        if (!this.lines.isEmpty()) {
            add(LogLine.marker("The application was restarted"));
        }
    }

    synchronized void add(LogLine line) {
        this.lines.addLast(line);
        if (this.lines.size() > CAPACITY) {
            this.lines.removeFirst();
        }
        // A follower that doesn't keep up misses lines instead of holding the application up.
        this.followers.forEach(follower -> follower.offer(line));
    }

    /**
     * Passes the lines kept so far to the given consumer, then each new line as it is logged,
     * until the calling thread is interrupted.
     */
    public void follow(Consumer<LogLine> consumer) throws InterruptedException {
        BlockingQueue<LogLine> follower = new ArrayBlockingQueue<>(FOLLOWER_CAPACITY);
        synchronized (this) {
            follower.addAll(this.lines);
            this.followers.add(follower);
        }
        try {
            while (true) {
                consumer.accept(follower.take());
            }
        } finally {
            synchronized (this) {
                this.followers.remove(follower);
            }
        }
    }

    synchronized List<LogLine> lines() {
        return List.copyOf(this.lines);
    }

    synchronized int followerCount() {
        return this.followers.size();
    }

}
