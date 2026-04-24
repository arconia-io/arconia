package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.OsInfo;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HostResourceContributor}.
 */
@ExtendWith(MockitoExtension.class)
class HostResourceContributorTests {

    private final HostResourceContributor contributor = new HostResourceContributor(() -> "test-host");

    @Mock
    private OsInfo osInfo;

    @Mock
    private ResourceBuilder resourceBuilder;

    @BeforeEach
    void setUp() {
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
        var customContributor = new HostResourceContributor(() -> "");

        customContributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(HostResourceContributor.HOST_NAME, "");
    }

    @Test
    void shouldSkipHostNameWhenNull() {
        var customContributor = new HostResourceContributor(() -> null);

        customContributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(HostResourceContributor.HOST_NAME, null);
    }

    @Test
    void shouldContributeAllAttributesWhenAvailable() {
        when(osInfo.getArch()).thenReturn("aarch64");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(HostResourceContributor.HOST_ARCH, "aarch64");
        verify(resourceBuilder).put(HostResourceContributor.HOST_NAME, "test-host");
    }

}
