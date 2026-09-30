/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test.common;

import io.quarkus.test.junit.callback.QuarkusTestAfterEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNameLogging implements QuarkusTestBeforeEachCallback, QuarkusTestAfterEachCallback {

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        Logger log = LoggerFactory.getLogger(context.getTestInstance().getClass());
        log.info("Executing {}\n" +
                "----------------------------------------------------------------------------------", context.getTestMethod());
    }

    @Override
    public void afterEach(QuarkusTestMethodContext context) {
        Logger log = LoggerFactory.getLogger(context.getTestInstance().getClass());
        log.info("Execution of {} is finished\n" +
                "----------------------------------------------------------------------------------", context.getTestMethod());
    }
}
