package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.OsInfo;
import org.springframework.test.util.ReflectionTestUtils;

import io.arconia.core.info.HostInfo;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HostResourceContributor}.
 */
@ExtendWith(MockitoExtension.class)
class HostResourceContributorTests {

    private final HostResourceContributor contributor = new HostResourceContributor();

    @Mock
    private HostInfo hostInfo;

    @Mock
    private OsInfo osInfo;

    @Mock
    private ResourceBuilder resourceBuilder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contributor, "hostInfo", hostInfo);
        ReflectionTestUtils.setField(contributor, "osInfo", osInfo);
    }

    @Test
    void shouldContributeHostArchWhenAvailable() {
        when(osInfo.getArch()).thenReturn("aarch64");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(HostResourceContributor.HOST_ARCH, "aarch64");
    }

    @Test
    void shouldContributeHostNameWhenAvailable() {
        when(hostInfo.getName()).thenReturn("test-host");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(HostResourceContributor.HOST_NAME, "test-host");
    }

    @Test
    void shouldSkipHostArchWhenEmpty() {
        when(osInfo.getArch()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(HostResourceContributor.HOST_ARCH, "");
    }

    @Test
    void shouldSkipHostArchWhenNull() {
        when(osInfo.getArch()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(HostResourceContributor.HOST_ARCH, null);
    }

    @Test
    void shouldSkipHostNameWhenEmpty() {
        when(hostInfo.getName()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(HostResourceContributor.HOST_NAME, "");
    }

    @Test
    void shouldSkipHostNameWhenNull() {
        when(hostInfo.getName()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(HostResourceContributor.HOST_NAME, null);
    }

    @Test
    void shouldContributeAllAttributesWhenAvailable() {
        when(osInfo.getArch()).thenReturn("aarch64");
        when(hostInfo.getName()).thenReturn("test-host");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(HostResourceContributor.HOST_ARCH, "aarch64");
        verify(resourceBuilder).put(HostResourceContributor.HOST_NAME, "test-host");
    }

}
