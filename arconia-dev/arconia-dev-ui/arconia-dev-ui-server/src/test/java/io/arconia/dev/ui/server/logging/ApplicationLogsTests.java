package io.arconia.dev.ui.server.logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Unit tests for {@link ApplicationLogs}.
 */
class ApplicationLogsTests {

    private final ApplicationLogs logs = new ApplicationLogs();

    @Test
    void linesAreKeptInTheOrderTheyAreLogged() {
        this.logs.add(line("first"));
        this.logs.add(line("second"));

        assertThat(this.logs.lines()).extracting(LogLine::message).containsExactly("first", "second");
    }

    @Test
    void oldestLinesAreDroppedBeyondTheCapacity() {
        for (int i = 1; i <= ApplicationLogs.CAPACITY + 10; i++) {
            this.logs.add(line("line " + i));
        }

        assertThat(this.logs.lines()).hasSize(ApplicationLogs.CAPACITY);
        assertThat(this.logs.lines().getFirst().message()).isEqualTo("line 11");
        assertThat(this.logs.lines().getLast().message()).isEqualTo("line " + (ApplicationLogs.CAPACITY + 10));
    }

    @Test
    void logsAreNotCapturedUntilTheCaptureStarts() {
        assertThat(this.logs.isCapturing()).isFalse();

        this.logs.startCapturing();

        assertThat(this.logs.isCapturing()).isTrue();
        assertThat(this.logs.lines()).isEmpty();
    }

    @Test
    void linesOfAnEarlierRunAreFollowedByMarkerWhenTheCaptureStartsAgain() {
        this.logs.startCapturing();
        this.logs.add(line("first run"));

        this.logs.startCapturing();
        this.logs.add(line("second run"));

        assertThat(this.logs.lines()).extracting(LogLine::message)
                .containsExactly("first run", "The application was restarted", "second run");
        assertThat(this.logs.lines()).extracting(LogLine::marker).containsExactly(false, true, false);
    }

    @Test
    void followerGetsTheLinesKeptSoFarThenEachNewLineUntilItIsInterrupted() throws Exception {
        this.logs.add(line("before"));
        List<String> received = new CopyOnWriteArrayList<>();
        Thread follower = Thread.ofVirtual().start(() -> {
            try {
                this.logs.follow(line -> received.add(line.message()));
            } catch (InterruptedException ex) {
                // Stopped following.
            }
        });
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(received).containsExactly("before"));

        this.logs.add(line("after"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> assertThat(received).containsExactly("before", "after"));
        follower.interrupt();
        follower.join(5000);
        assertThat(follower.isAlive()).isFalse();
        assertThat(this.logs.followerCount()).isZero();
    }

    @Test
    void addingLineDoesNotWaitForFollowerThatHasStoppedReading() throws Exception {
        Thread follower = Thread.ofVirtual().start(() -> {
            try {
                this.logs.follow(line -> {
                    try {
                        Thread.sleep(60_000);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (InterruptedException ex) {
                // Stopped following.
            }
        });
        await().atMost(5, TimeUnit.SECONDS).until(() -> this.logs.followerCount() == 1);

        long start = System.nanoTime();
        for (int i = 0; i < 20_000; i++) {
            this.logs.add(line("line " + i));
        }

        assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start)).isLessThan(5);
        follower.interrupt();
    }

    @Test
    void logsOfTheJvmAreShared() {
        assertThat(ApplicationLogs.get()).isSameAs(ApplicationLogs.get());
    }

    private static LogLine line(String message) {
        return new LogLine(System.currentTimeMillis(), "INFO", "com.example.Shop", message, null, false);
    }

}
