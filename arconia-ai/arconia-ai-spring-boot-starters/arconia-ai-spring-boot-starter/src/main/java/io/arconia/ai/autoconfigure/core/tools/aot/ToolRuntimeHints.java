package io.arconia.ai.autoconfigure.core.tools.aot;

import java.util.Set;
import java.util.function.Predicate;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Registers runtime hints for tool classes.
 */
public class ToolRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(@NonNull RuntimeHints hints, @Nullable ClassLoader classLoader) {
        var mcs = MemberCategory.values();

        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class, true));
        scanner.addIncludeFilter(new ToolAnnotationPresentFilter());

        for (var beanDefinition : scanner.findCandidateComponents(getApplicationBasePackage())) {
            try {
                var type = Class.forName(beanDefinition.getBeanClassName(), true, classLoader);
                hints.reflection().registerType(type, mcs);
            }
            catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to load class: " + beanDefinition.getBeanClassName(), e);
            }
        }
    }

    private String getApplicationBasePackage() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(SpringBootApplication.class));

        Set<BeanDefinition> mainAppCandidates = scanner.findCandidateComponents("");

        String applicationClassName = mainAppCandidates.iterator().next().getBeanClassName();
        Assert.hasText(applicationClassName, "applicationClassName cannot be null or empty");
        return applicationClassName.substring(0, applicationClassName.lastIndexOf('.'));
    }

    private static class ToolAnnotationPresentFilter implements TypeFilter {

        private static final Predicate<String> IS_TOOL_ANNOTATION = it -> it.startsWith("io.arconia.ai.core.tools");

        @Override
        public boolean match(MetadataReader reader, MetadataReaderFactory factory) {
            var annotationMetadata = reader.getAnnotationMetadata();
            return isClassAnnotated(annotationMetadata) || isMethodAnnotated(annotationMetadata);
        }

        private boolean isClassAnnotated(AnnotationMetadata annotationMetadata) {
            return annotationMetadata.getAnnotationTypes().stream().anyMatch(IS_TOOL_ANNOTATION);
        }

        private static boolean isMethodAnnotated(AnnotationMetadata annotationMetadata) {
            return annotationMetadata.getDeclaredMethods()
                .stream()
                .flatMap(it -> it.getAnnotations().stream())
                .map(MergedAnnotation::getType)
                .map(Class::getName)
                .anyMatch(IS_TOOL_ANNOTATION);
        }

    }

}
