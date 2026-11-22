package io.arconia.dev.ui.server.error;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Finds the endpoints the application defines with Spring MVC controllers, leaving out the
 * ones that the libraries it uses bring along.
 */
final class ApplicationEndpoints {

    private ApplicationEndpoints() {}

    /**
     * The endpoints of the controllers in the packages of the application, sorted by path.
     * When the packages of the application are not known, the endpoints of Spring itself are
     * the only ones left out. A path is the one under the context path of the application: it
     * starts with the path of Spring MVC, when one is configured.
     */
    static List<Endpoint> find(ApplicationContext applicationContext) {
        List<String> packages = AutoConfigurationPackages.has(applicationContext)
                ? AutoConfigurationPackages.get(applicationContext) : List.of();
        DispatcherServletPath dispatcherServletPath = applicationContext.getBeanProvider(DispatcherServletPath.class).getIfAvailable();
        String prefix = (dispatcherServletPath != null) ? dispatcherServletPath.getPrefix() : "";
        return applicationContext.getBeansOfType(RequestMappingHandlerMapping.class).values().stream()
                .flatMap(mapping -> mapping.getHandlerMethods().entrySet().stream())
                .filter(entry -> belongsToTheApplication(entry.getValue().getBeanType(), packages))
                .flatMap(entry -> entry.getKey().getPatternValues().stream()
                        .map(path -> new Endpoint(methods(entry.getKey().getMethodsCondition().getMethods()), prefix + path)))
                .distinct()
                .sorted(Comparator.comparing(Endpoint::path).thenComparing(Endpoint::methods))
                .toList();
    }

    private static boolean belongsToTheApplication(Class<?> controllerType, List<String> packages) {
        String packageName = controllerType.getPackageName();
        if (packages.isEmpty()) {
            return !packageName.startsWith("org.springframework.");
        }
        return packages.stream().anyMatch(name -> packageName.equals(name) || packageName.startsWith(name + "."));
    }

    private static String methods(Set<RequestMethod> methods) {
        return methods.stream().map(RequestMethod::name).sorted().collect(Collectors.joining(", "));
    }

    /**
     * An endpoint of the application.
     *
     * @param methods the HTTP methods the endpoint answers to, or an empty text when it
     * answers to all of them
     * @param path the path of the endpoint under the context path of the application
     */
    record Endpoint(String methods, String path) {

        /**
         * Whether the endpoint can be opened from a link: it answers to GET requests, and its
         * path has no variable or pattern in it.
         */
        boolean canBeOpened() {
            boolean answersToGet = this.methods.isEmpty() || this.methods.contains(RequestMethod.GET.name());
            return answersToGet && !this.path.contains("{") && !this.path.contains("*");
        }

    }

}
