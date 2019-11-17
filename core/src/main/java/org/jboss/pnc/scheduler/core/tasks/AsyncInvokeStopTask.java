package org.jboss.pnc.scheduler.core.tasks;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.axle.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.pnc.scheduler.core.ServiceControllerImpl;
import org.jboss.pnc.scheduler.core.exceptions.ConcurrentUpdateException;
import org.jboss.pnc.scheduler.core.exceptions.RetryException;
import org.jboss.pnc.scheduler.core.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.enterprise.inject.spi.CDI;
import javax.transaction.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.BooleanSupplier;

public class AsyncInvokeStopTask extends TransactionalControllerTask{
    private WebClient client;
    private URI uri;
    private Service service;
    private ServiceControllerImpl controller;

    private static final Logger logger = LoggerFactory.getLogger(AsyncInvokeStopTask.class);

    public AsyncInvokeStopTask(TransactionManager tm, Service service, ServiceControllerImpl controller) {
        super(tm);
        this.service = service;
        this.controller = controller;
        try {
            this.uri = new URI(service.getRemoteEndpoints().getStopUrl());
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
                .putHeader("content-type", "text/plain")
                .sendBuffer(Buffer.buffer(service.getPayload()))
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
