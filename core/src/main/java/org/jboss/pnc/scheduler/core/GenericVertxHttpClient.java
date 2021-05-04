package org.jboss.pnc.scheduler.core;


import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.jboss.pnc.scheduler.common.enums.Method;
import org.jboss.pnc.scheduler.model.Header;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

@ApplicationScoped
public class GenericVertxHttpClient {

    private final WebClient client;

    public GenericVertxHttpClient(Vertx vertx) {
        this.client = WebClient.create(vertx);
    }

    public void makeRequest(URI remoteEndpoint,
                             Method method,
                             List<Header> headers,
                             Object requestBody,
                             Consumer<HttpResponse<Buffer>> onResponse) {
        HttpRequest<Buffer> request = client.raw(method.toString(),
                remoteEndpoint.getPort() == -1 ? 80 : remoteEndpoint.getPort(),
                remoteEndpoint.getHost(),
                remoteEndpoint.getPath());
        addHeaders(request, headers);

        request.sendJson(requestBody)
                .onItem().transformToUni(i ->
                    Uni.createFrom()
                        .item(i).onItem().invoke(onResponse)
                        .onFailure().retry().atMost(5))
                .onFailure().retry().atMost(5)
                .subscribe().with(resp -> {});
    }

    private void addHeaders(HttpRequest<Buffer> request, List<Header> headers) {
        if (headers != null) {
            headers.forEach(header -> request.putHeader(header.getName(), header.getValue()));
        }
    }
}
