package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import io.arconia.core.annotations.Internal;
import io.arconia.dev.ui.api.page.LogSource;

/**
 * Streams logs to the browser as server-sent events, for as long as someone is watching.
 * <p>
 * Each viewer is served on the thread of its request. The log is followed on a thread of its
 * own, which passes the lines on through a bounded queue, so that a browser reading slowly
 * holds back the log it is watching and nothing else.
 * <p>
 * The streams are closed as soon as the application context starts to stop. Otherwise, the
 * web server would wait for them to end on their own before shutting down, and they never do.
 */
@Internal
public final class DevUiLogs implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(DevUiLogs.class);

    private static final String CONTENT_TYPE = new MediaType(MediaType.TEXT_EVENT_STREAM, StandardCharsets.UTF_8).toString();

    private static final int QUEUE_CAPACITY = 1000;

    /**
     * How long a stream stays silent before a comment is sent to find out whether the browser
     * is still there.
     */
    private static final long HEARTBEAT_SECONDS = 15;

    private static final byte[] HEARTBEAT = ":\n\n".getBytes(StandardCharsets.UTF_8);

    private final Set<Viewer> viewers = ConcurrentHashMap.newKeySet();

    private volatile boolean running;

    /**
     * Follows the given log and writes its lines to the response until the log ends, the
     * browser goes away, or the application stops.
     */
    void write(LogSource source, HttpServletResponse response) throws IOException {
        if (!this.running) {
            writeFailure("The application is stopping", response);
            return;
        }
        startStream(response);

        Viewer viewer = new Viewer(Thread.currentThread());
        this.viewers.add(viewer);
        Thread follower = Thread.ofVirtual().name("arconia-dev-ui-log").start(() -> follow(source, viewer));
        try {
            OutputStream output = response.getOutputStream();
            while (this.running) {
                Object next = viewer.queue.poll(HEARTBEAT_SECONDS, TimeUnit.SECONDS);
                if (next == null) {
                    output.write(HEARTBEAT);
                } else if (next instanceof End end) {
                    output.write(event(end.failure() == null ? "end" : "failure", end.failure() == null ? "" : end.failure()));
                    output.flush();
                    return;
                } else {
                    output.write(event(null, (String) next));
                }
                // Lines that arrived together are sent together.
                if (viewer.queue.isEmpty()) {
                    output.flush();
                }
            }
        } catch (IOException ex) {
            // The browser closed the stream.
        } catch (InterruptedException ex) {
            // The application is stopping. The interruption is consumed, since the thread goes
            // back to the web server.
        } finally {
            viewer.leave();
            this.viewers.remove(viewer);
            follower.interrupt();
        }
    }

    /**
     * Answers a request for a log that cannot be followed with a stream that only tells why. A
     * browser doesn't read the body of an answer that is not a stream, and would only say that
     * the connection failed.
     */
    void writeFailure(String reason, HttpServletResponse response) throws IOException {
        startStream(response);
        OutputStream output = response.getOutputStream();
        output.write(event("failure", reason));
        output.flush();
    }

    private static void startStream(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(CONTENT_TYPE);
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        response.flushBuffer();
    }

    private static void follow(LogSource source, Viewer viewer) {
        try {
            source.follow(viewer::accept);
            viewer.accept(new End(null));
        } catch (InterruptedException ex) {
            // Nobody is watching anymore.
        } catch (Exception | LinkageError ex) {
            if (!viewer.gone) {
                logger.warn("A log shown in the Dev UI cannot be followed", ex);
                viewer.accept(new End(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName()));
            }
        }
    }

    /**
     * A server-sent event, with one data line for each line of the given text.
     */
    private static byte[] event(@Nullable String name, String data) {
        StringBuilder event = new StringBuilder();
        if (name != null) {
            event.append("event: ").append(name).append('\n');
        }
        data.lines().forEach(line -> event.append("data: ").append(line).append('\n'));
        if (data.isEmpty()) {
            event.append("data:\n");
        }
        return event.append('\n').toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void start() {
        this.running = true;
    }

    @Override
    public void stop() {
        this.running = false;
        this.viewers.forEach(Viewer::interrupt);
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    /**
     * Someone watching a log: the thread serving them, and the lines waiting to be sent.
     */
    private static final class Viewer {

        private final Thread thread;

        private final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        private volatile boolean gone;

        private Viewer(Thread thread) {
            this.thread = thread;
        }

        /**
         * Interrupts the thread serving the viewer, unless it has already moved on. The thread
         * belongs to the web server, and must not be interrupted while it serves something else.
         */
        private synchronized void interrupt() {
            if (!this.gone) {
                this.thread.interrupt();
            }
        }

        /**
         * Called by the thread serving the viewer when it is done, which also clears an
         * interruption it has not noticed yet.
         */
        private synchronized void leave() {
            this.gone = true;
            Thread.interrupted();
        }

        /**
         * Waits for room in the queue, for as long as the viewer is there.
         */
        private void accept(Object item) {
            try {
                while (!this.gone && !this.queue.offer(item, 200, TimeUnit.MILLISECONDS)) {
                    // Wait for the browser to catch up.
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

    }

    /**
     * The end of a log, with the reason when it could not be followed any longer.
     */
    private record End(@Nullable String failure) {}

}
