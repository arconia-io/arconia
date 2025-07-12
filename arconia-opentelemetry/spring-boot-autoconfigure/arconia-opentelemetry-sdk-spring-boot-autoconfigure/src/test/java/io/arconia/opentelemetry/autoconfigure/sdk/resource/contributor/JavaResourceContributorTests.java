package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.JavaInfo;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JavaResourceContributor}.
 */
@ExtendWith(MockitoExtension.class)
class JavaResourceContributorTests {

    private final JavaResourceContributor contributor = new JavaResourceContributor();

    @Mock
    private JavaInfo javaInfo;

    @Mock
    private JavaInfo.JavaRuntimeEnvironmentInfo runtime;

    @Mock
    private JavaInfo.JavaVirtualMachineInfo jvm;

    @Mock
    private ResourceBuilder resourceBuilder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contributor, "javaInfo", javaInfo);
        when(javaInfo.getRuntime()).thenReturn(runtime);
        when(javaInfo.getJvm()).thenReturn(jvm);
    }

    @Test
    void shouldContributeRuntimeNameWhenAvailable() {
        when(runtime.getName()).thenReturn("OpenJDK Runtime Environment");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(JavaResourceContributor.PROCESS_RUNTIME_NAME, "OpenJDK Runtime Environment");
    }

    @Test
    void shouldContributeRuntimeVersionWhenAvailable() {
        when(runtime.getVersion()).thenReturn("17.0.1");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(JavaResourceContributor.PROCESS_RUNTIME_VERSION, "17.0.1");
    }

    @Test
    void shouldContributeRuntimeDescriptionWhenAllAttributesAvailable() {
        when(jvm.getVendor()).thenReturn("Eclipse Adoptium");
        when(jvm.getName()).thenReturn("OpenJDK Runtime Environment");
        when(jvm.getVersion()).thenReturn("17.0.1");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(JavaResourceContributor.PROCESS_RUNTIME_DESCRIPTION,
            "Eclipse Adoptium OpenJDK Runtime Environment 17.0.1");
    }

    @Test
    void shouldSkipRuntimeNameWhenEmpty() {
        when(runtime.getName()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(JavaResourceContributor.PROCESS_RUNTIME_NAME, "");
    }

    @Test
    void shouldSkipRuntimeNameWhenNull() {
        when(runtime.getName()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(JavaResourceContributor.PROCESS_RUNTIME_NAME, null);
    }

    @Test
    void shouldSkipRuntimeVersionWhenEmpty() {
        when(runtime.getVersion()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(JavaResourceContributor.PROCESS_RUNTIME_VERSION, "");
    }

    @Test
    void shouldSkipRuntimeVersionWhenNull() {
        when(runtime.getVersion()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(JavaResourceContributor.PROCESS_RUNTIME_VERSION, null);
    }

    @Test
    void shouldSkipRuntimeDescriptionWhenVendorEmpty() {
        when(jvm.getVendor()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(eq(JavaResourceContributor.PROCESS_RUNTIME_DESCRIPTION), anyString());
    }

    @Test
    void shouldSkipRuntimeDescriptionWhenVendorNull() {
        when(jvm.getVendor()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(eq(JavaResourceContributor.PROCESS_RUNTIME_DESCRIPTION), anyString());
    }

    @Test
    void shouldSkipRuntimeDescriptionWhenNameEmpty() {
        when(jvm.getVendor()).thenReturn("Eclipse Adoptium");
        when(jvm.getName()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(eq(JavaResourceContributor.PROCESS_RUNTIME_DESCRIPTION), anyString());
    }

    @Test
    void shouldSkipRuntimeDescriptionWhenNameNull() {
        when(jvm.getVendor()).thenReturn("Eclipse Adoptium");
        when(jvm.getName()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(eq(JavaResourceContributor.PROCESS_RUNTIME_DESCRIPTION), anyString());
    }

    @Test
    void shouldSkipRuntimeDescriptionWhenVersionEmpty() {
        when(jvm.getVendor()).thenReturn("Eclipse Adoptium");
        when(jvm.getName()).thenReturn("OpenJDK Runtime Environment");
        when(jvm.getVersion()).thenReturn("");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(eq(JavaResourceContributor.PROCESS_RUNTIME_DESCRIPTION), anyString());
    }

    @Test
    void shouldSkipRuntimeDescriptionWhenVersionNull() {
        when(jvm.getVendor()).thenReturn("Eclipse Adoptium");
        when(jvm.getName()).thenReturn("OpenJDK Runtime Environment");
        when(jvm.getVersion()).thenReturn(null);

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder, never()).put(eq(JavaResourceContributor.PROCESS_RUNTIME_DESCRIPTION), anyString());
    }

    @Test
    void shouldContributeAllAttributesWhenAvailable() {
        when(runtime.getName()).thenReturn("OpenJDK Runtime Environment");
        when(runtime.getVersion()).thenReturn("17.0.1");
        when(jvm.getVendor()).thenReturn("Eclipse Adoptium");
        when(jvm.getName()).thenReturn("OpenJDK Runtime Environment");
        when(jvm.getVersion()).thenReturn("17.0.1");

        contributor.contribute(resourceBuilder);

        verify(resourceBuilder).put(JavaResourceContributor.PROCESS_RUNTIME_NAME, "OpenJDK Runtime Environment");
        verify(resourceBuilder).put(JavaResourceContributor.PROCESS_RUNTIME_VERSION, "17.0.1");
        verify(resourceBuilder).put(JavaResourceContributor.PROCESS_RUNTIME_DESCRIPTION,
            "Eclipse Adoptium OpenJDK Runtime Environment 17.0.1");
    }

}
