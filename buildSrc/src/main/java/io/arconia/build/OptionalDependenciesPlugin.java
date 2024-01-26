package io.arconia.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * From: https://github.com/spring-projects/spring-boot/blob/main/buildSrc/src/main/java/org/springframework/boot/build/optional/OptionalDependenciesPlugin.java
 */
public class OptionalDependenciesPlugin implements Plugin<Project> {

    /**
     * Name of the {@code optional} configuration.
     */
    public static final String OPTIONAL_CONFIGURATION_NAME = "optional";

    @Override
    public void apply(Project project) {
        Configuration optional = project.getConfigurations().create("optional");
        optional.setCanBeConsumed(false);
        optional.setCanBeResolved(false);
        project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> {
            SourceSetContainer sourceSets = project.getExtensions()
                    .getByType(JavaPluginExtension.class)
                    .getSourceSets();
            sourceSets.all((sourceSet) -> {
                project.getConfigurations()
                        .getByName(sourceSet.getCompileClasspathConfigurationName())
                        .extendsFrom(optional);
                project.getConfigurations()
                        .getByName(sourceSet.getRuntimeClasspathConfigurationName())
                        .extendsFrom(optional);
            });
        });
    }

}
