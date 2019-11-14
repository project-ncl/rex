package org.jboss.pnc.scheduler.core.tasks;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.axle.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.pnc.scheduler.core.ServiceControllerImpl;
import org.jboss.pnc.scheduler.core.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.enterprise.inject.spi.CDI;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.net.URI;
import java.net.URISyntaxException;

public class InvokeStopTask extends TransactionalControllerTask{
    private WebClient client;
    private URI uri;
    private Service service;
    private ServiceControllerImpl controller;
    private ThreadContext threadContext;
    private ManagedExecutor managedExecutor;

    private static final Logger logger = LoggerFactory.getLogger(InvokeStopTask.class);

    public InvokeStopTask(TransactionManager tm, Service service, ServiceControllerImpl controller) {
        super(tm);
        this.service = service;
        this.controller = controller;
        try {
            this.uri = new URI(service.getRemoteEndpoints().getStopUrl());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("invalid stop url", e);
        }
        Vertx vertx = CDI.current().select(Vertx.class).get();
        threadContext = CDI.current().select(ThreadContext.class).get();
        managedExecutor = CDI.current().select(ManagedExecutor.class).get();
        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost(uri.getHost()).setDefaultPort(uri.getPort()));
    }

    @Override
    boolean execute() {
        logger.debug("Invoking stop task");
        threadContext.withContextCapture(
                client.post(uri.toString())
                .putHeader("content-type", "text/plain")
                .sendBuffer(Buffer.buffer(service.getPayload()))
        ).thenApplyAsync(resp -> {
                try {
                    logger.debug(resp.toString());
                    waitForTransactionToEnd(tm.getTransaction());
                    tm.suspend();
                    tm.begin();
                    if (resp.statusCode() == 200) {
                        System.out.println("GOT 200");
                        controller.accept();
                    } else {
                        System.out.println("GOT " + resp.statusCode());
                        //TODO rollback and retry on concurrent exception
                        controller.fail();
                    }
                    tm.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return resp; }
            , managedExecutor);
        return true;
    }

    private static void waitForTransactionToEnd(Transaction transaction) {
        while (true) {
            try {
                if (!isInTransaction(transaction)) {
                    return;
                }
                System.out.println(transaction.toString());
                Thread.sleep(25);
            } catch (InterruptedException e) {
                //nope
            }
        }
    }

    private static boolean isInTransaction(Transaction transaction){
        if (transaction == null) {
            return false;
        }
        try {
            switch (transaction.getStatus()) {
                case Status.STATUS_COMMITTING:
                case Status.STATUS_PREPARING:
                case Status.STATUS_ROLLING_BACK:
                case Status.STATUS_PREPARED:
                case Status.STATUS_MARKED_ROLLBACK:
                case Status.STATUS_ACTIVE:
                    return true;
                case Status.STATUS_COMMITTED:
                case Status.STATUS_ROLLEDBACK:
                case Status.STATUS_UNKNOWN:
                case Status.STATUS_NO_TRANSACTION:
                    return false;
                default:
                    throw new IllegalStateException("Unexpected value: " + transaction.getStatus());
            }
        } catch (SystemException e) {
            throw new IllegalStateException("Transaction failed in unexpected way",e);

        }
    }
}
