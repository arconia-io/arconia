package io.arconia.dev.ui.server.error;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DevUiStackTrace}.
 */
class DevUiStackTraceTests {

    private static final List<String> APPLICATION_PACKAGES = List.of("com.example.shop");

    @Test
    void rootCauseIsTheLastExceptionInTheChainOfCauses() {
        Exception rootCause = new IllegalArgumentException("No such shelf");
        Exception exception = new RuntimeException("Request failed", new IllegalStateException("The catalog is closed", rootCause));

        DevUiStackTrace stackTrace = new DevUiStackTrace(exception, List.of());

        assertThat(stackTrace.rootCause()).isSameAs(rootCause);
        assertThat(stackTrace.hasCause()).isTrue();
    }

    @Test
    void exceptionWithoutCauseIsItsOwnRootCause() {
        Exception exception = new IllegalStateException("The catalog is closed");

        DevUiStackTrace stackTrace = new DevUiStackTrace(exception, List.of());

        assertThat(stackTrace.rootCause()).isSameAs(exception);
        assertThat(stackTrace.hasCause()).isFalse();
    }

    @Test
    void chainOfCausesThatLoopsBackStopsWhereItWouldStartOver() {
        Exception first = new IllegalStateException("first");
        Exception second = new IllegalStateException("second", first);
        first.initCause(second);

        DevUiStackTrace stackTrace = new DevUiStackTrace(first, List.of());

        assertThat(stackTrace.rootCause()).isSameAs(second);
        assertThat(stackTrace.rootCauseFirst()).startsWith("java.lang.IllegalStateException: second");
    }

    @Test
    void originalOrderIsTheOneJavaPrints() {
        assertThat(new DevUiStackTrace(exception(), List.of()).original()).isEqualTo("""
                java.lang.IllegalStateException: The catalog is closed
                \tat com.example.shop.BookController.find(BookController.java:20)
                \tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:963)
                \tat java.base/java.lang.Thread.run(Thread.java:1583)
                Caused by: java.lang.IllegalArgumentException: No such shelf
                \tat com.example.shop.Catalog.shelf(Catalog.java:42)
                \tat com.example.shop.BookController.find(BookController.java:18)
                \t... 2 more""");
    }

    @Test
    void rootCauseFirstShowsTheCommonFramesWithTheRootCause() {
        assertThat(new DevUiStackTrace(exception(), List.of()).rootCauseFirst()).isEqualTo("""
                java.lang.IllegalArgumentException: No such shelf
                \tat com.example.shop.Catalog.shelf(Catalog.java:42)
                \tat com.example.shop.BookController.find(BookController.java:18)
                \tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:963)
                \tat java.base/java.lang.Thread.run(Thread.java:1583)
                Wrapped by: java.lang.IllegalStateException: The catalog is closed
                \tat com.example.shop.BookController.find(BookController.java:20)
                \t... 2 common frames omitted""");
    }

    @Test
    void framesOfTheApplicationAreToldApartInBothOrders() {
        DevUiStackTrace stackTrace = new DevUiStackTrace(exception(), APPLICATION_PACKAGES);

        assertThat(stackTrace.original())
                .contains("<span class=\"application\">\tat com.example.shop.BookController.find(BookController.java:20)</span>")
                .contains("<span class=\"library\">\tat org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:963)</span>")
                .contains("<span class=\"library\">\tat java.base/java.lang.Thread.run(Thread.java:1583)</span>")
                .contains("\nCaused by: java.lang.IllegalArgumentException: No such shelf\n");
        assertThat(stackTrace.rootCauseFirst())
                .contains("<span class=\"application\">\tat com.example.shop.Catalog.shelf(Catalog.java:42)</span>")
                .contains("<span class=\"library\">\tat java.base/java.lang.Thread.run(Thread.java:1583)</span>")
                .contains("\nWrapped by: java.lang.IllegalStateException: The catalog is closed\n");
    }

    @Test
    void packageWithNameStartingLikeTheOneOfTheApplicationIsNotPartOfIt() {
        Exception exception = new IllegalStateException("The catalog is closed");
        exception.setStackTrace(new StackTraceElement[] { new StackTraceElement("com.example.shopping.Cart", "add", "Cart.java", 7) });

        assertThat(new DevUiStackTrace(exception, APPLICATION_PACKAGES).original())
                .contains("<span class=\"library\">\tat com.example.shopping.Cart.add(Cart.java:7)</span>");
    }

    @Test
    void textOfTheExceptionIsEscaped() {
        Exception exception = new IllegalStateException("<script>alert(1)</script>", new IllegalArgumentException("<b>cause</b>"));
        DevUiStackTrace stackTrace = new DevUiStackTrace(exception, APPLICATION_PACKAGES);

        assertThat(stackTrace.original()).doesNotContain("<script>").doesNotContain("<b>");
        assertThat(stackTrace.rootCauseFirst()).doesNotContain("<script>").doesNotContain("<b>");
    }

    /**
     * An exception thrown by a controller of the application, caused by one thrown by the code
     * the controller called.
     */
    private static Exception exception() {
        StackTraceElement dispatch = new StackTraceElement(null, null, null, "org.springframework.web.servlet.DispatcherServlet",
                "doDispatch", "DispatcherServlet.java", 963);
        StackTraceElement thread = new StackTraceElement(null, "java.base", null, "java.lang.Thread", "run", "Thread.java", 1583);
        Exception cause = new IllegalArgumentException("No such shelf");
        cause.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.example.shop.Catalog", "shelf", "Catalog.java", 42),
                new StackTraceElement("com.example.shop.BookController", "find", "BookController.java", 18),
                dispatch, thread });
        Exception exception = new IllegalStateException("The catalog is closed", cause);
        exception.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("com.example.shop.BookController", "find", "BookController.java", 20),
                dispatch, thread });
        return exception;
    }

}
