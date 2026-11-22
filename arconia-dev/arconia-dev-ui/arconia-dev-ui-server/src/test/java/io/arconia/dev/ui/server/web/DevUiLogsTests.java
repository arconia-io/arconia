package io.arconia.dev.ui.server.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.ServletOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.mock.web.MockHttpServletResponse;

import io.arconia.dev.ui.api.page.LogSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiLogs}.
 */
@ExtendWith(OutputCaptureExtension.class)
class DevUiLogsTests {

    private final DevUiLogs logs = new DevUiLogs();

    @BeforeEach
    void start() {
        this.logs.start();
    }

    @AfterEach
    void stop() {
        this.logs.stop();
    }

    @Test
    void linesAreSentAsEventsUntilTheLogEnds() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.logs.write(lines -> {
            lines.accept("database system is ready");
            lines.accept("");
            lines.accept("first\nsecond");
        }, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("text/event-stream;charset=UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString()).isEqualTo("""
                data: database system is ready

                data:

                data: first
                data: second

                event: end
                data:

                """);
    }

    @Test
    void whenTheLogCannotBeFollowedThenTheFailureIsReported(CapturedOutput output) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.logs.write(lines -> {
            lines.accept("starting");
            throw new IllegalStateException("container not found");
        }, response);

        assertThat(response.getContentAsString()).isEqualTo("""
                data: starting

                event: failure
                data: container not found

                """);
        assertThat(output).contains("A log shown in the Dev UI cannot be followed");
    }

    @Test
    void whenTheApplicationStopsThenTheStreamIsClosedAndTheLogIsNoLongerFollowed() throws Exception {
        CountDownLatch following = new CountDownLatch(1);
        CountDownLatch released = new CountDownLatch(1);
        LogSource endless = lines -> {
            lines.accept("started");
            following.countDown();
            try {
                new CountDownLatch(1).await();
            } finally {
                released.countDown();
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean interruptedAfterwards = new AtomicBoolean(true);
        Thread request = Thread.ofPlatform().start(() -> {
            try {
                this.logs.write(endless, response);
                interruptedAfterwards.set(Thread.currentThread().isInterrupted());
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
        assertThat(following.await(5, TimeUnit.SECONDS)).isTrue();

        this.logs.stop();

        request.join(5000);
        assertThat(request.isAlive()).isFalse();
        assertThat(interruptedAfterwards).isFalse();
        assertThat(released.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(response.getContentAsString()).startsWith("data: started\n\n");
    }

    @Test
    void whenTheBrowserGoesAwayThenTheLogIsNoLongerFollowed() throws Exception {
        CountDownLatch released = new CountDownLatch(1);
        LogSource endless = lines -> {
            lines.accept("started");
            try {
                new CountDownLatch(1).await();
            } finally {
                released.countDown();
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse() {

            @Override
            public ServletOutputStream getOutputStream() {
                return new DelegatingServletOutputStream(OutputStream.nullOutputStream()) {

                    @Override
                    public void write(byte[] bytes) throws IOException {
                        throw new IOException("Broken pipe");
                    }

                };
            }

        };

        this.logs.write(endless, response);

        assertThat(released.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void whenTheApplicationIsStoppingThenNoLogIsFollowed() throws Exception {
        this.logs.stop();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean followed = new AtomicBoolean();

        this.logs.write(lines -> followed.set(true), response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("event: failure\ndata: The application is stopping\n\n");
        assertThat(followed).isFalse();
    }

}
