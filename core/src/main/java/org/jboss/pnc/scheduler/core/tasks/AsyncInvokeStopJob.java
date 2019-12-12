package org.jboss.pnc.scheduler.core.tasks;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.ext.web.client.WebClient;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import org.jboss.pnc.scheduler.core.TaskControllerImpl;
import org.jboss.pnc.scheduler.common.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.scheduler.common.exceptions.RetryException;
import org.jboss.pnc.scheduler.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.enterprise.inject.spi.CDI;
import javax.transaction.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BooleanSupplier;

public class AsyncInvokeStopJob extends SynchronizedAsyncControllerJob {
    private WebClient client;
    private URI uri;
    private Task task;
    private TaskControllerImpl controller;
    private String schedulerBaseUrl;

    private static final Logger logger = LoggerFactory.getLogger(AsyncInvokeStopJob.class);

    public AsyncInvokeStopJob(TransactionManager tm, Task task, TaskControllerImpl controller, String schedulerBaseUrl) {
        super(tm);
        this.task = task;
        this.controller = controller;
        this.schedulerBaseUrl = schedulerBaseUrl;
        try {
            this.uri = new URI(task.getRemoteEndpoints().getStopUrl());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("invalid stop url", e);
        }
        Vertx vertx = CDI.current().select(Vertx.class).get();
        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost(uri.getHost()).setDefaultPort(uri.getPort()));
    }

    @Override
    boolean execute() {
        logger.debug("Invoking stop task");
        client.post(uri.toString())
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(new JsonObject()
                        .put("callback", schedulerBaseUrl + "/rest/internal/"+ task.getName().getCanonicalName() + "/finish")
                        .put("payload", task.getPayload())
                )
                .thenApply(resp -> {
                            try {
                                logger.debug(resp.toString());
                                if (resp.statusCode() == 200) {
                                    System.out.println("GOT 200 on " + controller.getName());
                                    retry(10, () -> wrapInTransactionAndHandle(() -> controller.accept()));
                                } else {
                                    System.out.println("GOT " + resp.statusCode());
                                    retry(10, () -> wrapInTransactionAndHandle(() -> controller.fail()));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return resp;
                        }
                );
        return true;
    }

    private static void retry(int times, BooleanSupplier shouldRetry) {
        for (int i = 0; i < times; i++) {
            try {
                if (!shouldRetry.getAsBoolean())
                    return;
                Thread.sleep(50);
            } catch (InterruptedException e) {
                //shouldn't happen
            }
        }
        throw new RetryException("Enough is enough!");
    }

    /*
      returns true if the invocation should be retried
     */
    private boolean wrapInTransactionAndHandle(Runnable runnable) {
        try {
            tm.begin();
            runnable.run();
            tm.commit();
        } catch (IllegalStateException | ConcurrentUpdateException e) {
            try {
                tm.rollback();
                return true;
            } catch (SystemException ex) {
                ex.printStackTrace();
            }
        } catch (RollbackException e) {
            logger.debug("Transaction rolled back ");
            return true;
        } catch (HeuristicRollbackException | NotSupportedException | HeuristicMixedException | SystemException e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }
}
