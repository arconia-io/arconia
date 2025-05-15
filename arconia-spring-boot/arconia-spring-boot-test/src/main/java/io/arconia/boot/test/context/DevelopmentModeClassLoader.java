package io.arconia.boot.test.context;

import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestContext;

import io.arconia.core.support.Incubating;

/**
 * A class loader that filters out the classes heuristically used
 * when detecting application modes other than development mode.
 */
@Incubating
public class DevelopmentModeClassLoader extends FilteredClassLoader {

    public DevelopmentModeClassLoader() {
        super(TestContext.class, SpringBootTest.class);
    }

    public DevelopmentModeClassLoader(ClassLoader parentClassLoader) {
        super(parentClassLoader, TestContext.class, SpringBootTest.class);
    }

}
