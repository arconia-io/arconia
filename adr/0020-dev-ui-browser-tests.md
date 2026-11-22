# 20. Dev UI browser tests

Date: 2026-11-22

## Status

Accepted

## Context

The Dev UI frontend is written in JavaScript with no build in front of it (see [17. Dev UI frontend](0017-dev-ui-frontend.md)), and some of its behavior exists only in the browser: routing between pages, filtering a table and keeping the filter in the address, following a log. The tests of the server check what it sends, not what the browser does with it. A mistake in a script shows up only when a person opens the page.

Testing in a browser has to fit the choices behind the frontend. The tests are written and run by Java developers, in the Gradle build and in continuous integration, with no JavaScript toolchain next to it. And the pages are made of web components, Vaadin components and Lit elements, most of whose structure lives in shadow roots.

Several tools were considered.

* Selenium, with the browser in a container from the Testcontainers Selenium module. It's plain Java and talks to the browser over HTTP, with nothing else running next to the test. But it doesn't look inside shadow roots by itself: a test has to walk into each component to reach a field or a button, and depends on how the component is built inside.
* HtmlUnit. It runs in the JVM with no browser, but it doesn't support JavaScript modules, import maps, and custom elements well enough to run the Dev UI.
* Vaadin TestBench. It knows the Vaadin components, but it needs a commercial license, which doesn't suit an open-source project.
* A JavaScript test runner, such as Vitest or Web Test Runner. It would test the scripts closely, but it brings back the toolchain that the frontend avoids.
* Playwright. Its Java library is published on Maven Central and comes with support for JUnit. Its locators find elements by role, label, or text through shadow roots, and its assertions wait for the page to reach the expected state. Microsoft publishes a container image with the browsers and the system libraries they need.

## Decision

The Dev UI frontend is tested with Playwright for Java, in JUnit tests that are part of the Gradle build.

The browser runs in a container. Testcontainers starts the official Playwright image and runs the Playwright server in it, once for all the browser tests. The tests connect to it through the JUnit support of Playwright, and the version of the Playwright library on the class path picks the version of the image and of the server, because the two sides must match. The browser reaches the application under test on `localhost` through the connection to the test, so the Dev UI sees requests coming from the local machine, as it does when a developer opens it. Playwright doesn't download browsers of its own.

The tests start an application with the Dev UI, open its pages in Chromium, and act on them as a developer does: through the roles and labels of the components, never through their inner structure. Every test also checks that the browser reported no errors.

## Consequences

Building and testing Arconia still takes a JDK and a container runtime, and nothing else. The tests are Java code, upgraded like any other dependency.

Behavior that only the browser has is now covered, and a regression fails the build. A test that fails leaves a trace of what the browser did, which the Playwright trace viewer opens.

The Java library of Playwright drives the browser through a driver of its own that runs on Node.js. The driver is packaged in a jar for every platform and started by the library, so nothing is installed on the machine, but the jar is large, a Node.js process runs during the tests, and the Playwright server in the container is fetched from npm when the container starts.

The browser tests need a container runtime, like the other integration tests of Arconia, and the Playwright image is large to pull the first time.

The tests run in Chromium only. The support of Playwright for JUnit is marked as experimental. If it changes, the tests keep their shape and only the way they get a browser changes.

The setup is part of the tests of the Dev UI server. Making it available to applications that want to test their own frontend is left for later.
