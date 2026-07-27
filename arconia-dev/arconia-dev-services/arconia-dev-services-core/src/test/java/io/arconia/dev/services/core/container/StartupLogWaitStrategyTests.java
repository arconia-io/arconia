package io.arconia.dev.services.core.container;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link StartupLogWaitStrategy}.
 */
@ExtendWith(OutputCaptureExtension.class)
class StartupLogWaitStrategyTests {

    @Test
    void whenDelegateFailsThenContainerLogsAreDumpedAndFailurePropagates(CapturedOutput output) {
        WaitStrategyTarget target = mock(WaitStrategyTarget.class);
        given(target.getLogs()).willReturn("failing container output");
        WaitStrategy delegate = failingDelegate();

        var strategy = new StartupLogWaitStrategy(delegate, "lgtm", LogLevel.INFO);

        assertThatThrownBy(() -> strategy.waitUntilReady(target))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("startup timeout");
        assertThat(output).contains("lgtm").contains("failing container output");
    }

    @Test
    void whenDelegateSucceedsThenNothingIsDumped(CapturedOutput output) {
        WaitStrategyTarget target = mock(WaitStrategyTarget.class);

        new StartupLogWaitStrategy(succeedingDelegate(), "lgtm", LogLevel.INFO).waitUntilReady(target);

        assertThat(output).doesNotContain("failed to start");
        verify(target, never()).getLogs();
    }

    @Test
    void whenLevelIsOffThenNothingIsDumpedButFailurePropagates(CapturedOutput output) {
        WaitStrategyTarget target = mock(WaitStrategyTarget.class);

        var strategy = new StartupLogWaitStrategy(failingDelegate(), "lgtm", LogLevel.OFF);

        assertThatThrownBy(() -> strategy.waitUntilReady(target)).isInstanceOf(IllegalStateException.class);
        assertThat(output).doesNotContain("failed to start");
        // The container is not queried for its logs when logging is disabled.
        verify(target, never()).getLogs();
    }

    private static WaitStrategy failingDelegate() {
        return new WaitStrategy() {
            @Override
            public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
                throw new IllegalStateException("startup timeout");
            }

            @Override
            public WaitStrategy withStartupTimeout(Duration startupTimeout) {
                return this;
            }
        };
    }

    private static WaitStrategy succeedingDelegate() {
        return new WaitStrategy() {
            @Override
            public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
                // ready
            }

            @Override
            public WaitStrategy withStartupTimeout(Duration startupTimeout) {
                return this;
            }
        };
    }

}
