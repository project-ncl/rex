package org.jboss.pnc.scheduler.core.tasks;

import io.vertx.axle.core.Vertx;
import io.vertx.axle.core.buffer.Buffer;
import io.vertx.axle.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.jboss.pnc.scheduler.core.ServiceControllerImpl;
import org.jboss.pnc.scheduler.core.model.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.enterprise.inject.spi.CDI;
import javax.transaction.TransactionManager;
import java.net.URI;
import java.net.URISyntaxException;

public class InvokeStopTask extends TransactionalControllerTask{
    private WebClient client;
    private URI uri;
    private Service service;
    private ServiceControllerImpl controller;

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
        client = WebClient.create(vertx, new WebClientOptions().setDefaultHost(uri.getHost()).setDefaultPort(uri.getPort()));
    }

    @Override
    boolean execute() {
        logger.debug("Invoking s");
        client.post(uri.toString())
                .sendBuffer(Buffer.buffer(service.getPayload()))
                .thenApplyAsync(resp -> {
                    if (resp.statusCode() == 200) {
                        controller.accept();
                    } else {
                        controller.fail();
                    }
                    return null;
                });
        return true;
    }
}
