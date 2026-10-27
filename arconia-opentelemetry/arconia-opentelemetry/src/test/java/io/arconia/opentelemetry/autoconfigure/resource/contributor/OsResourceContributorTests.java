package io.arconia.opentelemetry.autoconfigure.resource.contributor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
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

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contributor, "osInfo", osInfo);
        ReflectionTestUtils.setField(contributor, "osReleasePaths", List.of(tempDir.resolve("etc-os-release"), tempDir.resolve("usr-lib-os-release")));
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
    void shouldContributeOsDescriptionWithNameAndVersionWhenAvailable() {
        when(osInfo.getName()).thenReturn("Mac OS X");
        when(osInfo.getVersion()).thenReturn("14.2.1");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Mac OS X 14.2.1");
    }

    @Test
    void shouldContributeOsDescriptionFromOsReleaseOnLinux() throws IOException {
        Files.writeString(tempDir.resolve("etc-os-release"), """
                NAME="Ubuntu"
                VERSION_ID="24.04"
                PRETTY_NAME="Ubuntu 24.04.1 LTS"
                ID=ubuntu
                """);
        when(osInfo.getName()).thenReturn("Linux");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Ubuntu 24.04.1 LTS");
    }

    @Test
    void shouldContributeOsDescriptionFromFallbackOsReleaseOnLinux() throws IOException {
        Files.writeString(tempDir.resolve("usr-lib-os-release"), "PRETTY_NAME='Debian GNU/Linux 12 (bookworm)'\n");
        when(osInfo.getName()).thenReturn("Linux");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Debian GNU/Linux 12 (bookworm)");
    }

    @Test
    void shouldContributeOsDescriptionWithNameAndVersionOnLinuxWithoutOsRelease() {
        when(osInfo.getName()).thenReturn("Linux");
        when(osInfo.getVersion()).thenReturn("6.8.0");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Linux 6.8.0");
    }

    @Test
    void shouldIgnoreOsReleaseOnOtherOperatingSystems() throws IOException {
        Files.writeString(tempDir.resolve("etc-os-release"), "PRETTY_NAME=\"Ubuntu 24.04.1 LTS\"\n");
        when(osInfo.getName()).thenReturn("Mac OS X");
        when(osInfo.getVersion()).thenReturn("14.2.1");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Mac OS X 14.2.1");
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
        when(osInfo.getName()).thenReturn("Mac OS X");
        when(osInfo.getVersion()).thenReturn("14.2.1");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_NAME, "Mac OS X");
        verify(resourceBuilder).put(OsResourceContributor.OS_TYPE, "darwin");
        verify(resourceBuilder).put(OsResourceContributor.OS_VERSION, "14.2.1");
        verify(resourceBuilder).put(OsResourceContributor.OS_DESCRIPTION, "Mac OS X 14.2.1");
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
    void shouldContributeOsTypeForSolarisReportedAsSunOs() {
        when(osInfo.getName()).thenReturn("SunOS");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_TYPE, "solaris");
    }

    @Test
    void shouldContributeOsTypeIndependentlyOfDefaultLocale() {
        Locale defaultLocale = Locale.getDefault();
        try {
            // In Turkish, "I" lowercases to the dotless "ı", so "AIX" wouldn't match "aix"
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            when(osInfo.getName()).thenReturn("AIX");

            contributor.contribute(resourceBuilder);

            verify(resourceBuilder).put(OsResourceContributor.OS_TYPE, "aix");
        }
        finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    void shouldContributeOriginalNameWhenOsTypeUnknown() {
        when(osInfo.getName()).thenReturn("CustomOS");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(OsResourceContributor.OS_TYPE, "CustomOS");
    }

}
