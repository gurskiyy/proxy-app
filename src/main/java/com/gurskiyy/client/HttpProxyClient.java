package com.gurskiyy.client;

import com.gurskiyy.exception.ProxyException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class HttpProxyClient {

    private final HttpClient client;

    @Inject
    public HttpProxyClient(HttpClient client) {
        this.client = client;
    }

    public HttpResponse<byte[]> sendRequest(URI targetUri) {
        try {
            HttpRequest request = HttpRequest.newBuilder().uri(targetUri).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProxyException("Interrupted: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new ProxyException("Error forwarding request: " + e.getMessage(), e);
        }
    }
}
