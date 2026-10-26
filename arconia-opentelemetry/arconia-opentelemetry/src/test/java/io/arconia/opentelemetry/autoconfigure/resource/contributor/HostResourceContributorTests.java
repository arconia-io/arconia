package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.OsInfo;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
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

        verify(resourceBuilder).put(HostResourceContributor.HOST_ARCH, "arm64");
    }

    @ParameterizedTest
    @CsvSource({
            "amd64, amd64", "x86_64, amd64",
            "aarch64, arm64", "arm64, arm64",
            "arm, arm32",
            "x86, x86", "i386, x86", "i686, x86",
            "ia64, ia64",
            "ppc, ppc32", "ppc64, ppc64", "ppc64le, ppc64",
            "s390x, s390x",
            "riscv64, riscv64"
    })
    void shouldMapArchitectureToHostArchValues(String osArch, String hostArch) {
        assertThat(HostResourceContributor.toHostArch(osArch)).isEqualTo(hostArch);
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

        verify(resourceBuilder).put(HostResourceContributor.HOST_ARCH, "arm64");
        verify(resourceBuilder).put(HostResourceContributor.HOST_NAME, "test-host");
    }

}
