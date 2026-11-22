# 18. Dev UI server

Date: 2026-11-20

## Status

Accepted

## Context

The Dev UI (see [16. Dev UI](0016-dev-ui.md)) is served by the application it describes. The obvious way to serve something from a Spring Boot application is the way the application serves its own pages: Spring MVC controllers, behind the filters of the application.

That web layer belongs to the application, and the application shapes it. It can move Spring MVC to another path, change how JSON is written, add interceptors, replace the handling of errors, and turn off the serving of static files. A Dev UI built on it would behave differently from one application to the next, and its own controllers and settings would in turn become part of what the application has to live with.

The filters in front of it belong to the application too. With Spring Security, every path requires a login unless the application says otherwise, so the Dev UI would not open until the developer changed the security configuration of their application for a development tool. The two ways for a library to make an exception on its own both change the application: a security filter chain for the Dev UI makes Spring Boot drop the default one that protects the application, and telling Spring Security to ignore the path logs a warning at startup. Other filters have the same effect, such as one that rejects the requests it cannot match to a tenant.

At the same time, the Dev UI shows what should not leave the machine of the developer: configuration, logs, stack traces. A web server listens on every network interface by default, so the one of a development machine can be reached by any other machine on the same network. It can also be reached by any web page open in the browser of the developer, either with requests sent across sites or through a host name that the page controls and points at the local machine (DNS rebinding).

The Quarkus Dev UI, which inspired this one, talks to the application with JSON-RPC over a WebSocket. Spring has no support for JSON-RPC.

## Decision

The Dev UI has its own servlet, at a fixed path under the context path of the application, `/a/dev-ui`. It doesn't use Spring MVC. It routes its requests, writes its JSON, and serves its files and the frontend libraries (see [17. Dev UI frontend](0017-dev-ui-frontend.md)) by itself, so nothing the application configures for its own web layer changes the Dev UI, and the Dev UI adds nothing to it.

Its requests are answered by a filter of its own, ordered ahead of Spring Security and the other filters of the application, which hands them to the servlet directly. Only the few filters that Spring Boot orders even earlier, such as the one that applies forwarded headers and the one that observes requests, see them. The security of the application stays exactly as the application defined it, for all its own paths. The Dev UI has no login, and none is planned.

What protects it is where a request comes from and what it is addressed to. The Dev UI only accepts requests that come from the local machine and are addressed to it, and rejects the ones sent by a page of another website. Other host names, such as the one of a remote development environment, have to be allowed with `arconia.dev.ui.allowed-hosts`, and other machines, as for an application in a container, with `arconia.dev.ui.remote-access.enabled`. An allowed host is accepted from any machine, so the names of the local machine cannot be allowed hosts: listing one would quietly let other machines in. The host is taken from the request itself, and where a request comes from is the machine that opened the connection: the headers that a proxy adds are never enough on their own, because a web page, or any machine on the network, can set them too. The Dev UI also forbids other websites from showing it inside a frame.

What changes something, such as restarting the application, has to prove more: that it comes from the Dev UI itself. It must carry a header of the Dev UI, which a page of another website, or of another application on the local machine, cannot add without the browser asking the Dev UI for permission first, which the Dev UI never gives.

The Dev UI keeps to itself in the other direction as well. It answers its own errors and never hands a failed request to the error handling of the application. When it cannot start, for example because a frontend library is missing, it says so and the application starts without it. Its requests are left out of the metrics and traces of the application, unless `arconia.dev.ui.observations.enabled` says otherwise.

The browser talks to it over plain HTTP: JSON for what a page shows, and server-sent events for what keeps arriving, such as the lines of a log. A stream that cannot be followed says why in an event of its own, since a browser doesn't read the body of a failed stream. A stream stays open for as long as someone watches it and is closed when the application stops, so that it never delays the shutdown.

The logs of the application are kept in memory from the moment it starts, up to a fixed number of lines, so that the Dev UI can show what was logged before anyone opened it. They are kept across the restarts of Spring Boot DevTools, where the run that just ended is often what the developer wants to read. Every answer of the Dev UI tells when the application started, which is how the browser notices a restart and loads the Dev UI again.

What the Dev UI shows about the application, it reads with the same code Spring Boot uses for the Actuator endpoints, where there is such code, starting with the rules that decide which values are sensitive. The Dev UI depends on the Actuator library for that, not on its auto-configuration, and adds, enables, or exposes no endpoint. A sensitive value is sent to the browser with the rest of its page and hidden on screen until the developer asks to see it. Fetching it only at that moment would protect nothing more: without a login, whoever can read a page can ask for the value too. Hiding it is for what the check on where a request comes from doesn't cover: someone looking at the screen, a screenshot, a shared screen.

The welcome page and the error pages are the one place where the Dev UI is part of the web layer of the application, because that is where the whitelabel error page they replace is. They step in only when the application would show that page, and follow the same rule about where a request comes from.

## Consequences

The Dev UI opens in any servlet application as soon as the dependency is added, whatever the application does with Spring MVC, Spring Security, or its filters, and adding it changes none of them.

Arconia writes and maintains what Spring MVC would otherwise provide: routing, JSON, static files, caching headers, error responses. Each new kind of request is a little more of that code.

Anyone who can send a request the Dev UI accepts can use it, without a login. By default that is someone on the development machine itself: the developer. Allowing another host name, or other machines, extends that to everyone who can reach the application that way, which is why each is an explicit choice. An application in a container doesn't get a Dev UI until it makes that choice.

The checks are as good as what the web server lets the Dev UI see. When the web server applies forwarded headers itself (`server.forward-headers-strategy=native`, which Spring Boot also does by default on a cloud platform), it replaces the address of a request, and on Jetty its host too, before the Dev UI can look at them. The Dev UI then takes no request to come from the local machine: the requests addressed to it are rejected unless remote access is enabled, and a warning at startup says so. When Spring applies the forwarded headers instead (`framework`), nothing changes.

The Actuator library is on the class path of the application in dev mode even when the application doesn't use Actuator. On its own it changes nothing, with one known exception: an application that has the health support of Spring Boot without Actuator, which the Spring Boot starters don't lead to, gets the beans of the health endpoint in dev mode. The endpoint is not exposed.

The path is taken: whatever the application maps under `/a/dev-ui` is not reachable in dev mode.

Server-sent events go one way, and each stream takes one of the few connections a browser opens to a server. Features that need the browser to talk back over the same channel, or many streams at once, will need a WebSocket. The servlet can upgrade a request to one. The same holds for a future endpoint for tools other than a browser, such as an MCP server: it would be another servlet behind the same check.

Every action that changes the application goes through the same check, so adding one doesn't reopen the question of who may ask for it.
