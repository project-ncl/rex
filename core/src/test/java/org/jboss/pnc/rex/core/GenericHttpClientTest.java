package org.jboss.pnc.rex.core;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.jboss.pnc.rex.api.TaskEndpoint;
import org.jboss.pnc.rex.common.enums.Mode;
import org.jboss.pnc.rex.common.enums.State;
import org.jboss.pnc.rex.core.common.TestData;
import org.jboss.pnc.rex.core.common.TransitionRecorder;
import org.jboss.pnc.rex.core.endpoints.HttpEndpoint;
import org.jboss.pnc.rex.dto.requests.CreateGraphRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jboss.pnc.rex.core.common.Assertions.waitTillTasksAreFinishedWith;
import static org.jboss.pnc.rex.core.common.TestData.createMockTask;
import static org.jboss.pnc.rex.core.common.TestData.getRequestFromSingleTask;

@QuarkusTest
@TestSecurity(authorizationEnabled = false)
public class GenericHttpClientTest {

    @Inject
    TaskContainerImpl container;

    @Inject
    HttpEndpoint endpoint;

    @Inject
    TaskEndpoint taskEndpoint;

    @Inject
    TransitionRecorder recorder;

    @AfterEach
    public void after() throws InterruptedException {
        endpoint.clearRequestCounter();
        container.getCache().clear();
        recorder.clear();
        Thread.sleep(100);
    }

    @Test
    void shouldRetryBackpressureOn425AndSucceed() {
        int amountOf425UntilSuccessful = 5;
        CreateGraphRequest request = getRequestFromSingleTask(createMockTask(
                "backoff-test",
                Mode.ACTIVE,
                TestData.getRequestWithBackoff(null, amountOf425UntilSuccessful),
                TestData.getStopRequest(null),
                null));

        taskEndpoint.start(request);
        waitTillTasksAreFinishedWith(State.SUCCESSFUL, "backoff-test");

        assertThat(endpoint.getCount()).isEqualTo(amountOf425UntilSuccessful);
    }

    @Test
    void shouldRetryOn425AndFailToStart() {
        int amountOf425UntilSuccessful = Integer.MAX_VALUE; // from application.yaml expiry for fallback is set to 5sec
        CreateGraphRequest request = getRequestFromSingleTask(createMockTask(
                "backoff-test",
                Mode.ACTIVE,
                TestData.getRequestWithBackoff(null, amountOf425UntilSuccessful),
                TestData.getStopRequest(null),
                null));

        taskEndpoint.start(request);
        waitTillTasksAreFinishedWith(State.START_FAILED, "backoff-test");

        assertThat(endpoint.getCount()).isNotZero();
    }
}
