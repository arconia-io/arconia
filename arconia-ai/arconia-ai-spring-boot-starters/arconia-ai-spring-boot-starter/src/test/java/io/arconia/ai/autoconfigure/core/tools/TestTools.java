package io.arconia.ai.autoconfigure.core.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.arconia.ai.tools.annotation.Tool;

@Component
public class TestTools {

    private static final Logger logger = LoggerFactory.getLogger(TestTools.class);

    @Tool("Welcome users to the library")
    public void welcome() {
        logger.info("Welcoming users to the library");
    }

}
