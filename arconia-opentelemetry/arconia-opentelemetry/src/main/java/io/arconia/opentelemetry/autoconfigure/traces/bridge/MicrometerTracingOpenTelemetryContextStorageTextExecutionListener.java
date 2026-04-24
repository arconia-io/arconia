/*
 * Copyright 2012-present the original author or authors.
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

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

/**
 * JUnit {@link TestExecutionListener} to ensure
 * {@link MicrometerTracingOpenTelemetryContextStorageApplicationListener#addWrapper()} is called as
 * early as possible.
 * <p>
 * Code adapted from <a href="https://github.com/spring-projects/spring-boot/blob/v4.0.1/module/spring-boot-micrometer-tracing-opentelemetry/src/main/java/org/springframework/boot/micrometer/tracing/opentelemetry/autoconfigure/OpenTelemetryEventPublisherBeansTestExecutionListener.java">OpenTelemetryEventPublisherBeansTestExecutionListener.java</a>
 * authored by Phillip Webb in the Spring Boot project.
 */
public class MicrometerTracingOpenTelemetryContextStorageTextExecutionListener implements TestExecutionListener {

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        MicrometerTracingOpenTelemetryContextStorageApplicationListener.addWrapper();
    }

}
