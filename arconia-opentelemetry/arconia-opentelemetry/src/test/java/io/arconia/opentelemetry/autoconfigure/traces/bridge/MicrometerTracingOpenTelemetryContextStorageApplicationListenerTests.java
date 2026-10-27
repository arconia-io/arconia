package io.arconia.opentelemetry.autoconfigure.traces.bridge;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.support.SpringFactoriesLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Tests that optional Micrometer tracing support does not prevent the OpenTelemetry module from
 * being used without micrometer-tracing-bridge-otel on the consuming application's classpath.
 */
class MicrometerTracingOpenTelemetryContextStorageApplicationListenerTests {

    private static final String LISTENER_PACKAGE = "io.arconia.opentelemetry.autoconfigure.traces.bridge.";

    private static final String MICROMETER_BRIDGE_PACKAGE = "io.micrometer.tracing.otel.bridge.";

    private static final String TEST_EXECUTION_LISTENER_RESOURCE =
            "META-INF/services/org.junit.platform.launcher.TestExecutionListener";

    @Test
    void applicationListenerLoadsAndHandlesEventsWithoutMicrometerTracingBridge() throws Exception {
        ClassLoader classLoader = new MicrometerTracingBridgeClassLoader(getClass().getClassLoader(), false);
        List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(ApplicationListener.class, classLoader);
        String listenerName = factoryNames.stream()
                .filter(name -> name.startsWith(LISTENER_PACKAGE))
                .findFirst()
                .orElseThrow();
        Class<?> listenerClass = classLoader.loadClass(listenerName);
        @SuppressWarnings("unchecked")
        ApplicationListener<ApplicationEvent> listener =
                (ApplicationListener<ApplicationEvent>) instantiate(listenerClass);

        assertThatCode(() -> listener.onApplicationEvent(new ContextRefreshedEvent(mock(ApplicationContext.class))))
                .doesNotThrowAnyException();
    }

    @Test
    void applicationListenerDelegatesWhenMicrometerTracingBridgeIsPresent() throws Exception {
        ClassLoader classLoader = new MicrometerTracingBridgeClassLoader(getClass().getClassLoader(), true);
        List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(ApplicationListener.class, classLoader);
        String listenerName = factoryNames.stream()
                .filter(name -> name.startsWith(LISTENER_PACKAGE))
                .findFirst()
                .orElseThrow();
        Class<?> listenerClass = classLoader.loadClass(listenerName);
        @SuppressWarnings("unchecked")
        ApplicationListener<ApplicationEvent> listener =
                (ApplicationListener<ApplicationEvent>) instantiate(listenerClass);

        assertThatCode(() -> listener.onApplicationEvent(new ContextRefreshedEvent(mock(ApplicationContext.class))))
                .doesNotThrowAnyException();
    }

    @SuppressWarnings("deprecation")
    @Test
    void junitListenerCanRunWithoutMicrometerBridge() throws Exception {
        ClassLoader classLoader = new MicrometerTracingBridgeClassLoader(getClass().getClassLoader(), false);
        List<String> listenerNames = new ArrayList<>();
        var resources = classLoader.getResources(TEST_EXECUTION_LISTENER_RESOURCE);
        while (resources.hasMoreElements()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    resources.nextElement().openStream(), StandardCharsets.UTF_8))) {
                reader.lines()
                        .map(String::trim)
                        .filter(name -> !name.isEmpty() && !name.startsWith("#"))
                        .filter(name -> name.startsWith(LISTENER_PACKAGE))
                        .forEach(listenerNames::add);
            }
        }
        assertThat(listenerNames).isNotEmpty();
        Class<?> listenerClass = classLoader.loadClass(listenerNames.get(0));
        TestExecutionListener listener = (TestExecutionListener) instantiate(listenerClass);

        TestIdentifier testIdentifier = TestIdentifier.from(
                new EngineDescriptor(UniqueId.forEngine("arconia-test"), "Arconia test"));
        assertThatCode(() -> listener.executionStarted(testIdentifier)).doesNotThrowAnyException();
    }

    private static Object instantiate(Class<?> type) throws Exception {
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static final class MicrometerTracingBridgeClassLoader extends ClassLoader {

        private final boolean bridgeAvailable;

        private MicrometerTracingBridgeClassLoader(ClassLoader parent, boolean bridgeAvailable) {
            super(parent);
            this.bridgeAvailable = bridgeAvailable;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!this.bridgeAvailable && name.startsWith(MICROMETER_BRIDGE_PACKAGE)) {
                throw new ClassNotFoundException(name);
            }
            if (!name.startsWith(LISTENER_PACKAGE)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    String resourceName = name.replace('.', '/') + ".class";
                    try (InputStream input = getParent().getResourceAsStream(resourceName)) {
                        if (input == null) {
                            throw new ClassNotFoundException(name);
                        }
                        byte[] bytes = input.readAllBytes();
                        loaded = defineClass(name, bytes, 0, bytes.length);
                    }
                    catch (IOException exception) {
                        throw new ClassNotFoundException(name, exception);
                    }
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

    }

}
