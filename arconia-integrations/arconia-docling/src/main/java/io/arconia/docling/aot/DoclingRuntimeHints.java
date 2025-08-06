package io.arconia.docling.aot;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;

import io.arconia.docling.client.DoclingClient;

/**
 * Register runtime hints for Docling.
 */
class DoclingRuntimeHints implements RuntimeHintsRegistrar {

    private static final Logger logger = LoggerFactory.getLogger(DoclingRuntimeHints.class);

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        Assert.notNull(hints, "hints cannot be null");
        var mcs = MemberCategory.values();
        for (var tr : findAllJsonAnnotatedClassesInPackage(DoclingClient.class.getPackageName())) {
            hints.reflection().registerType(tr, mcs);
        }
    }

    private Set<TypeReference> findAllJsonAnnotatedClassesInPackage(String packageName) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(JsonInclude.class));
        return scanner.findCandidateComponents(packageName).stream()
                .map(beanDef -> TypeReference.of(Objects.requireNonNull(beanDef.getBeanClassName())))
                .peek(typeRef -> logger.debug("Found @JsonInclude class: {}", typeRef.getName()))
                .collect(Collectors.toUnmodifiableSet());
    }

}
