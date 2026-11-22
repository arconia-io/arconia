# 17. Dev UI frontend

Date: 2026-11-20

## Status

Accepted

## Context

The Dev UI (see [16. Dev UI](0016-dev-ui.md)) is a web interface shipped inside a Java library. That sets it apart from the frontend of an application in three ways.

It is built and maintained by Java developers, as part of a Gradle build. A JavaScript toolchain next to it (Node.js, a package manager, a bundler) would be a second build to keep working, in the project and in its continuous integration, for a feature that is a small part of Arconia.

It runs inside somebody else's application. Whatever it needs at run time ends up on the class path of that application, next to the libraries the application has chosen for its own frontend.

Its content comes from many modules, and it will grow page by page. It needs a set of ready-made components (layout, navigation, cards, dialogs, tables, form fields) with a consistent look in light and dark mode, so that a new page is mostly a matter of arranging them.

Several ways of building it were considered.

* Server-side templates with Thymeleaf. Everything stays in Java and HTML, but a template engine becomes a dependency of the application and shares its configuration when the application uses Thymeleaf too. It brings no components: the layout, the widgets, and their styles would all be written and maintained in Arconia. Live content, such as a log being followed, needs scripts on top of it anyway.
* Server-side templates with htmx. It adds partial updates and server-sent events to the previous option with very little JavaScript. The pages are still rendered by a template engine on the server, and there is still no component library.
* A single-page application with React. It has the largest choice of components and tools, and it is what most web developers know. It needs the JavaScript toolchain described above, with a large tree of dependencies to keep up to date, and its output would have to be built before the Java build can package it.
* Vaadin Flow. The user interface is written in Java and Vaadin takes care of the browser, which fits a Java library well. But Flow keeps its configuration in the servlet context, which all the servlets of an application share, so an application can only have one Flow setup. A library cannot bring a Flow interface of its own without getting in the way of an application that uses Vaadin for its own frontend, and both would have to agree on one Vaadin version. Members of the Vaadin team confirmed the limitation in [vaadin/flow#23088](https://github.com/vaadin/flow/discussions/23088).

Web components are part of the browser. The components that Vaadin Flow renders are web components, published on their own and usable without Flow. Browsers load JavaScript modules directly and resolve the names of the libraries they import through an import map, which is what a bundler used to be needed for. [mvnpm](https://mvnpm.org) republishes the packages of the npm registry as jars on Maven Central, each one with the import map entry of its library.

## Decision

The Dev UI is built with web components, and it has no JavaScript build.

The components come from Vaadin: its web components, with the Aura theme. The elements of the Dev UI itself (the page shell, a page, a log viewer) are written with Lit, the library the Vaadin components are built on. The icons are the Lucide icon set.

The libraries are dependencies of the Java build like any other. They are declared as mvnpm artifacts, resolved from Maven Central, and served to the browser from their jars. The Dev UI merges the import maps of the libraries on the class path and writes the result into its page. Its own scripts and stylesheets are plain files in the source tree, served as they are. Nothing is transpiled, bundled, or minified, and neither Node.js nor npm is involved in building Arconia or an application that uses it.

The look comes from the theme. The Arconia brand (colors, heading font, logo) is defined in a stylesheet of its own, shared with the welcome and error pages, and applied through the style properties that Aura and the Vaadin components define. The body font is the one of Aura. A style of our own is written only where no property exists. The elements of the Dev UI render without a shadow root, because the theme is a stylesheet of the page and doesn't reach a Vaadin component inside the shadow root of another element. Their styles are in one stylesheet, where each rule starts with the name of the element it belongs to.

The server decides and the browser renders. What can be worked out in Java, such as how the navigation is grouped, which text is too long for a table, or how a time is written, is sent ready to show, and the scripts keep to what only the browser can do: routing, filtering and sorting what is on screen, following a log, and remembering the choices of the developer. A widget that Vaadin provides is used rather than written.

Every page has an address of its own, and what narrows a page down is part of that address: the filter of a table is. A view can therefore be bookmarked and reloaded, and one page can link to a view of another, as the card of a Dev Service links to its configuration properties. What a developer chooses for their own comfort, such as the color scheme or the height of the log console, is remembered in the browser instead.

The Vaadin components collect usage statistics when they run on a development machine. The Dev UI turns that off.

The welcome page and the error pages are the exception: they are plain HTML and CSS written by the server, with no library, so that they can be shown whatever the state of the rest of the Dev UI.

## Consequences

Building Arconia takes a JDK and nothing else. Upgrading a frontend library is a version change in the Java build, and tools that watch Maven dependencies cover the frontend too.

The Dev UI is written in JavaScript without a compiler or a type checker in front of it, and the browser is the first to see a mistake. The Java build checks what it can: that every import in the scripts and in the libraries leads to a file on the class path. Browser tests cover what happens on the page (see [20. Dev UI browser tests](0020-dev-ui-browser-tests.md)).

The browser loads each module as a separate file, which means many requests for a page. They come from the local machine and the library files are cached, so the page still opens quickly, but the approach would not suit a public website.

The frontend libraries are on the class path of the application in dev mode. An application that uses the same libraries through mvnpm shares their versions with the Dev UI, in one direction or the other.

What the Dev UI can show is bounded by the Vaadin components and by what Aura lets a theme change. A design that needs more is written and maintained in Arconia.

A module contributes content through a Java API and writes no frontend code. A module that needs an element of its own in the browser has no way to provide one yet.
