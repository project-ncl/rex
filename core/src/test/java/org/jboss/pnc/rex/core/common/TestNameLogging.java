package org.jboss.pnc.rex.core.common;

import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNameLogging implements QuarkusTestBeforeEachCallback {

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        Logger log = LoggerFactory.getLogger(context.getTestInstance().getClass());
        log.info("Executing " + context.getTestMethod());
    }
}
