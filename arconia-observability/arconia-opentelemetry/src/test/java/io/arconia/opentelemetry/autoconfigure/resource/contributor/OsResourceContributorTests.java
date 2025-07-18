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
 * Unit tests for {@link OsResourceContributor}.
 */
@ExtendWith(MockitoExtension.class)
class OsResourceContributorTests {

    private final OsResourceContributor contributor = new OsResourceContributor();

    @Mock
    private OsInfo osInfo;

    @Mock
    private ResourceBuilder resourceBuilder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contributor, "osInfo", osInfo);
    }

    @Test
    void shouldContributeOsNameWhenAvailable() {
        when(osInfo.getName()).thenReturn("Mac OS X");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_NAME, "Mac OS X");
    }

    @Test
    void shouldContributeOsTypeWhenNameAvailable() {
        when(osInfo.getName()).thenReturn("Mac OS X");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_TYPE, "darwin");
    }

    @Test
    void shouldContributeOsVersionWhenAvailable() {
        when(osInfo.getVersion()).thenReturn("14.2.1");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_VERSION, "14.2.1");
    }

    @Test
    void shouldContributeOsDescriptionWithAllAttributesWhenAvailable() {
        when(osInfo.getName()).thenReturn("Mac OS X");
        when(osInfo.getVersion()).thenReturn("14.2.1");
        when(osInfo.getArch()).thenReturn("aarch64");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Mac OS X [Version: 14.2.1, Architecture: aarch64]");
    }

    @Test
    void shouldContributeOsDescriptionWithNameAndVersionWhenArchUnavailable() {
        when(osInfo.getName()).thenReturn("Mac OS X");
        when(osInfo.getVersion()).thenReturn("14.2.1");
        when(osInfo.getArch()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Mac OS X [Version: 14.2.1]");
    }

    @Test
    void shouldContributeOsDescriptionWithNameOnlyWhenOtherAttributesUnavailable() {
        when(osInfo.getName()).thenReturn("Mac OS X");
        when(osInfo.getVersion()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Mac OS X");
    }

    @Test
    void shouldSkipOsNameWhenEmpty() {
        when(osInfo.getName()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(OsResourceContributor.OS_NAME, "");
    }

    @Test
    void shouldSkipOsNameWhenNull() {
        when(osInfo.getName()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(OsResourceContributor.OS_NAME, null);
    }

    @Test
    void shouldSkipOsVersionWhenEmpty() {
        when(osInfo.getVersion()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(OsResourceContributor.OS_VERSION, "");
    }

    @Test
    void shouldSkipOsVersionWhenNull() {
        when(osInfo.getVersion()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(OsResourceContributor.OS_VERSION, null);
    }

    @Test
    void shouldContributeAllAttributesWhenAvailable() {
        when(osInfo.getArch()).thenReturn("aarch64");
        when(osInfo.getName()).thenReturn("Mac OS X");
        when(osInfo.getVersion()).thenReturn("14.2.1");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_NAME, "Mac OS X");
        verify(resourceBuilder).put(OsResourceContributor.OS_TYPE, "darwin");
        verify(resourceBuilder).put(OsResourceContributor.OS_VERSION, "14.2.1");
        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Mac OS X [Version: 14.2.1, Architecture: aarch64]");
    }

    @Test
    void shouldContributeOsTypeForLinux() {
        when(osInfo.getName()).thenReturn("Linux");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_TYPE, "linux");
    }

    @Test
    void shouldContributeOsTypeForWindows() {
        when(osInfo.getName()).thenReturn("Windows 11");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_TYPE, "windows");
    }

    @Test
    void shouldContributeOriginalNameWhenOsTypeUnknown() {
        when(osInfo.getName()).thenReturn("CustomOS");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_TYPE, "CustomOS");
    }

}
