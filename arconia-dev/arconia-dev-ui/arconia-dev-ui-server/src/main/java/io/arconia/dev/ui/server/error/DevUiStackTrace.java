package io.arconia.dev.ui.server.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.springframework.web.util.HtmlUtils;

/**
 * The stack trace of an exception as HTML, for the page of a failed request.
 * <p>
 * The exception that reaches the page is often a wrapper around the one that explains the
 * failure, which the usual stack trace shows last. The trace can be written starting from that
 * root cause instead. In both orders, the frames of the application are marked apart from the
 * ones of the libraries it uses, so that the code of the developer stands out.
 */
final class DevUiStackTrace {

    private static final String FRAME_PREFIX = "at ";

    private final Throwable exception;

    private final List<String> applicationPackages;

    DevUiStackTrace(Throwable exception, List<String> applicationPackages) {
        this.exception = exception;
        this.applicationPackages = applicationPackages;
    }

    /**
     * The exception at the origin of the failure: the last one in the chain of causes.
     */
    Throwable rootCause() {
        List<Throwable> chain = chain();
        return chain.getLast();
    }

    boolean hasCause() {
        return chain().size() > 1;
    }

    /**
     * The trace in the order Java prints it: the exception first, then each of its causes.
     */
    String original() {
        StringWriter stackTrace = new StringWriter();
        this.exception.printStackTrace(new PrintWriter(stackTrace));
        StringBuilder html = new StringBuilder();
        for (String line : stackTrace.toString().strip().split("\\R")) {
            String text = line.strip();
            if (text.startsWith(FRAME_PREFIX)) {
                appendFrame(html, line, className(text.substring(FRAME_PREFIX.length())));
            } else {
                html.append(escape(line)).append('\n');
            }
        }
        return html.toString().stripTrailing();
    }

    /**
     * The trace starting from the root cause, followed by each exception that wraps it. The
     * frames an exception has in common with the one it wraps are shown once, with the latter.
     */
    String rootCauseFirst() {
        List<Throwable> chain = chain().reversed();
        StringBuilder html = new StringBuilder();
        StackTraceElement[] causeFrames = new StackTraceElement[0];
        for (Throwable throwable : chain) {
            html.append((throwable == chain.getFirst()) ? "" : "Wrapped by: ").append(escape(throwable.toString())).append('\n');
            StackTraceElement[] frames = throwable.getStackTrace();
            int common = commonFrames(frames, causeFrames);
            for (int i = 0; i < frames.length - common; i++) {
                appendFrame(html, "\t" + FRAME_PREFIX + frames[i], frames[i].getClassName());
            }
            if (common > 0) {
                html.append("\t... ").append(common).append(" common frames omitted\n");
            }
            causeFrames = frames;
        }
        return html.toString().stripTrailing();
    }

    /**
     * The exception and its causes, in that order. A chain that loops back on itself stops
     * where it would start over.
     */
    private List<Throwable> chain() {
        List<Throwable> chain = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable throwable = this.exception; throwable != null && seen.add(throwable); throwable = throwable.getCause()) {
            chain.add(throwable);
        }
        return chain;
    }

    /**
     * How many frames two traces share, counting from the frame where the thread started.
     */
    private static int commonFrames(StackTraceElement[] frames, StackTraceElement[] otherFrames) {
        int common = 0;
        while (common < frames.length && common < otherFrames.length
                && frames[frames.length - 1 - common].equals(otherFrames[otherFrames.length - 1 - common])) {
            common++;
        }
        return common;
    }

    private void appendFrame(StringBuilder html, String line, String className) {
        if (this.applicationPackages.isEmpty()) {
            html.append(escape(line)).append('\n');
            return;
        }
        html.append("<span class=\"").append(belongsToTheApplication(className) ? "application" : "library").append("\">")
                .append(escape(line)).append("</span>\n");
    }

    private boolean belongsToTheApplication(String className) {
        return this.applicationPackages.stream().anyMatch(packageName -> className.startsWith(packageName + "."));
    }

    /**
     * The name of the class in a frame as Java prints it, which is the name of the method with
     * its class, sometimes after the names of the class loader and of the module, followed by
     * the location in the source.
     */
    private static String className(String frame) {
        int location = frame.indexOf('(');
        String method = (location >= 0) ? frame.substring(0, location) : frame;
        method = method.substring(method.lastIndexOf('/') + 1);
        int methodName = method.lastIndexOf('.');
        return (methodName >= 0) ? method.substring(0, methodName) : method;
    }

    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text);
    }

}
