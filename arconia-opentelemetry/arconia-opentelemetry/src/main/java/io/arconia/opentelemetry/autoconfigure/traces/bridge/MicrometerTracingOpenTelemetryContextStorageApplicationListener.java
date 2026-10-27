/*
 * Copyright 2025-2026 Arconia Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.arconia.opentelemetry.autoconfigure.traces.bridge;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.GenericApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * Loads the Micrometer tracing bridge listener only when its optional bridge dependency is present.
 */
public class MicrometerTracingOpenTelemetryContextStorageApplicationListener implements GenericApplicationListener {

    private static final String MICROMETER_EVENT_PUBLISHER_CLASS =
            "io.micrometer.tracing.otel.bridge.OtelTracer$EventPublisher";

    private static final String DELEGATE_CLASS = "io.arconia.opentelemetry.autoconfigure.traces.bridge."
            + "MicrometerTracingOpenTelemetryContextStorageApplicationListenerDelegate";

    private final ClassLoader classLoader;

    private volatile ListenerDelegate delegate;

    private volatile boolean delegateInitialized;

    public MicrometerTracingOpenTelemetryContextStorageApplicationListener() {
        this.classLoader = getClass().getClassLoader();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean supportsEventType(ResolvableType eventType) {
        Class<?> type = eventType.getRawClass();
        return (type != null) && (ApplicationStartingEvent.class.isAssignableFrom(type)
                || ContextRefreshedEvent.class.isAssignableFrom(type)
                || ContextClosedEvent.class.isAssignableFrom(type));
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        ListenerDelegate delegate = getDelegateIfAvailable();
        if (delegate != null) {
            delegate.onApplicationEvent(event);
        }
    }

    /**
     * Adds the optional Micrometer tracing context-storage wrapper when the bridge is present.
     */
    public void addWrapperIfAvailable() {
        ListenerDelegate delegate = getDelegateIfAvailable();
        if (delegate != null) {
            delegate.addWrapper();
        }
    }

    private ListenerDelegate getDelegateIfAvailable() {
        if (!this.delegateInitialized) {
            synchronized (this) {
                if (!this.delegateInitialized) {
                    if (ClassUtils.isPresent(MICROMETER_EVENT_PUBLISHER_CLASS, this.classLoader)) {
                        Class<?> delegateClass = ClassUtils.resolveClassName(DELEGATE_CLASS, this.classLoader);
                        this.delegate = (ListenerDelegate) BeanUtils.instantiateClass(delegateClass);
                    }
                    this.delegateInitialized = true;
                }
            }
        }
        return this.delegate;
    }

    interface ListenerDelegate extends GenericApplicationListener {

        void addWrapper();

    }

}
