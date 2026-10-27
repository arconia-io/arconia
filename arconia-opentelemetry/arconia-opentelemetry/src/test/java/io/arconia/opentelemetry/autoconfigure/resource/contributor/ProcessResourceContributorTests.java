package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import java.time.Instant;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.ProcessInfo;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProcessResourceContributor}.
 */
@ExtendWith(MockitoExtension.class)
class ProcessResourceContributorTests {

    private final ProcessResourceContributor contributor = new ProcessResourceContributor();

    @Mock
    private ProcessInfo processInfo;

    @Mock
    private ResourceBuilder resourceBuilder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contributor, "processInfo", processInfo);
    }

    @Test
    void shouldContributeProcessOwnerWhenAvailable() {
        when(processInfo.getOwner()).thenReturn("test-user");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_OWNER, "test-user");
    }

    @Test
    void shouldContributeParentPidWhenAvailable() {
        when(processInfo.getParentPid()).thenReturn(1234L);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_PARENT_PID, 1234L);
    }

    @Test
    void shouldSkipParentPidWhenNoParent() {
        when(processInfo.getParentPid()).thenReturn(-1L);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(eq(ProcessResourceContributor.PROCESS_PARENT_PID), anyLong());
    }

    @Test
    void shouldContributePidWhenAvailable() {
        when(processInfo.getPid()).thenReturn(5678L);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_PID, 5678L);
    }

    @Test
    void shouldContributeCreationTimeWhenAvailable() {
        when(processInfo.getStartTime()).thenReturn(Instant.parse("2023-11-21T09:25:34.853Z"));

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_CREATION_TIME, "2023-11-21T09:25:34.853Z");
    }

    @Test
    void shouldSkipCreationTimeWhenUnknown() {
        when(processInfo.getStartTime()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(eq(ProcessResourceContributor.PROCESS_CREATION_TIME), anyString());
    }

    @Test
    void shouldContributeWorkingDirectoryWhenAvailable() {
        when(processInfo.getWorkingDirectory()).thenReturn("/app");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_WORKING_DIRECTORY, "/app");
    }

    @Test
    void shouldSkipWorkingDirectoryWhenEmpty() {
        when(processInfo.getWorkingDirectory()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(eq(ProcessResourceContributor.PROCESS_WORKING_DIRECTORY), anyString());
    }

    @Test
    void shouldSkipProcessOwnerWhenEmpty() {
        when(processInfo.getOwner()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(ProcessResourceContributor.PROCESS_OWNER, "");
    }

    @Test
    void shouldSkipProcessOwnerWhenNull() {
        when(processInfo.getOwner()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(ProcessResourceContributor.PROCESS_OWNER, null);
    }

    @Test
    void shouldContributeAllAttributesWhenAvailable() {
        when(processInfo.getStartTime()).thenReturn(Instant.parse("2023-11-21T09:25:34.853Z"));
        when(processInfo.getOwner()).thenReturn("test-user");
        when(processInfo.getParentPid()).thenReturn(1234L);
        when(processInfo.getPid()).thenReturn(5678L);
        when(processInfo.getWorkingDirectory()).thenReturn("/app");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_CREATION_TIME, "2023-11-21T09:25:34.853Z");
        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_WORKING_DIRECTORY, "/app");
        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_OWNER, "test-user");
        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_PARENT_PID, 1234L);
        verify(resourceBuilder).put(ProcessResourceContributor.PROCESS_PID, 5678L);
    }

}
