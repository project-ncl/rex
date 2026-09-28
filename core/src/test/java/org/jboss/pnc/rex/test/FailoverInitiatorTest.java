/*
 * SPDX-FileCopyrightText: Copyright © 2021 Red Hat, Inc., and individual contributors as indicated by the @author tags.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.rex.test;

import io.quarkus.test.Mock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;
import io.smallrye.config.SmallRyeConfig;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.Config;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.CJobOperation;
import org.jboss.pnc.rex.common.enums.ResourceType;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.FailoverInitiator;
import org.jboss.pnc.rex.core.api.ClusteredJobRegistry;
import org.jboss.pnc.rex.core.api.TaskContainer;
import org.jboss.pnc.rex.core.config.ApplicationConfig;
import org.jboss.pnc.rex.core.jobs.cluster.ClusteredJob;
import org.jboss.pnc.rex.dto.ConfigurationDTO;
import org.jboss.pnc.rex.model.ClusteredJobReference;
import org.jboss.pnc.rex.model.NodeResource;
import org.jboss.pnc.rex.test.common.AbstractTest;
import org.jboss.pnc.rex.test.common.TestData;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.test.ClusteredJobsTest.taskID;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTaskTransitionsInto;
import static org.jboss.pnc.rex.test.common.Assertions.waitTillTasksAre;

@Slf4j
@QuarkusTest
public class FailoverInitiatorTest extends AbstractTest {

    @TestHTTPEndpoint(TaskEndpoint.class)
    @TestHTTPResource
    URI taskURI;

    @Inject
    TaskContainer container;

    @Inject
    FailoverInitiator initiator;

    @InjectSpy(convertScopes = true)
    ApplicationConfig appConfig; // Mockito-Spied real config

    @Inject
    ClusteredJobRegistry registry;

    /**
     * Override default Smallrye Config object scope which is @Dependent so that the config can be mocked.
     *
     * To be able to mock it. Use @InjectSpy(convertScopes = true).
     *
     * @return mock-able ApplicationConfig
     */
    @Produces
    @Mock
    @Singleton
    ApplicationConfig applicationConfig(Config config) {
        return config.unwrap(SmallRyeConfig.class).getConfigMapping(ApplicationConfig.class);
    }

    @Test
    void testResourcesRegisterOnShutdown() throws InterruptedException {
        // given
        String task = taskID();
        Duration timeoutValue = Duration.ofMillis(1000000);
        String referenceId = ClusteredJob.generateReferenceId(task, CJobOperation.CANCEL_TIMEOUT);

        var graph = TestData.getSingleWithoutStart(task);
        graph.graphConfiguration = new ConfigurationDTO();
        graph.graphConfiguration.cancelTimeout = timeoutValue;

        given()
            .contentType(ContentType.JSON)
            .body(graph)
            .when()
            .post(taskURI.getPath())
            .then()
            .statusCode(200);
        waitTillTaskTransitionsInto(State.UP, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);

        waitTillTaskTransitionsInto(State.STOPPING, task);
        waitt();

        // when
        initiator.failoverResources(); // force the method that is triggered on shutdown

        // then
        assertThat(initiator.signal()).isNotEmpty().containsKey(referenceId);
        NodeResource resource = initiator.signal().get(referenceId);
        assertThat(resource).isNotNull();
        assertThat(resource.getResourceId()).isEqualTo(referenceId);
        assertThat(resource.getOwnerNode()).isNotNull().isNotBlank();
        assertThat(resource.getResourceType()).isEqualTo(ResourceType.CLUSTERED_JOB);
    }

    @Test
    void testFailoverHappens() throws InterruptedException {
        // given
        String task = taskID();
        Duration timeoutValue = Duration.ofMillis(1000000);
        String referenceId = ClusteredJob.generateReferenceId(task, CJobOperation.CANCEL_TIMEOUT);

        var graph = TestData.getSingleWithoutStart(task);
        graph.graphConfiguration = new ConfigurationDTO();
        graph.graphConfiguration.cancelTimeout = timeoutValue;

        given()
            .contentType(ContentType.JSON)
            .body(graph)
            .when()
            .post(taskURI.getPath())
            .then()
            .statusCode(200);
        waitTillTaskTransitionsInto(State.UP, task);

        given().put(taskURI.getPath() + TaskEndpoint.CANCEL_PATH_FMT.formatted(task)).then().statusCode(202);
        waitTillTaskTransitionsInto(State.STOPPING, task);
        waitt(); // async stuff

        // when
        initiator.failoverResources();
        waitt(); // async stuff

        // force change instance name to simulate another instance that takes-over the resources
        Mockito.doReturn("very-nice-instance").when(appConfig).name();
        initiator.takeAvailableResources();
        waitt(); // async stuff

        // then
        NodeResource resource = initiator.signal().get(referenceId);
        assertThat(resource).isNull(); // resource should be taken

        // verify takeover happened
        ClusteredJobReference jobReference = registry.getById(referenceId);
        assertThat(jobReference.getOwner()).isEqualTo("very-nice-instance");
    }

    private void waitt() throws InterruptedException {
        Thread.sleep(50);
    }
}
