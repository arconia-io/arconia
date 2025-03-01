package io.arconia.opentelemetry.autoconfigure.sdk.resource.contributor;

import io.arconia.core.info.HostInfo;
import io.opentelemetry.sdk.resources.ResourceBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HostResourceContributor}.
 */
@ExtendWith(MockitoExtension.class)
class HostResourceContributorTests {

    private HostResourceContributor contributor = new HostResourceContributor();

    @Mock
    private HostInfo hostInfo;

    @Mock
    private ResourceBuilder resourceBuilder;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(contributor, "hostInfo", hostInfo);
    }

    @Test
    void shouldContributeHostArchWhenAvailable() {
        when(hostInfo.getArch()).thenReturn("aarch64");
        
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
        when(hostInfo.getArch()).thenReturn("");
        
        contributor.contribute(resourceBuilder);
        
        verify(resourceBuilder, never()).put(HostResourceContributor.HOST_ARCH, "");
    }

    @Test
    void shouldSkipHostArchWhenNull() {
        when(hostInfo.getArch()).thenReturn(null);
        
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
        when(hostInfo.getArch()).thenReturn("aarch64");
        when(hostInfo.getName()).thenReturn("test-host");
        
        contributor.contribute(resourceBuilder);
        
        verify(resourceBuilder).put(HostResourceContributor.HOST_ARCH, "aarch64");
        verify(resourceBuilder).put(HostResourceContributor.HOST_NAME, "test-host");
    }

}
