package com.gurskiyy.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.net.http.HttpClient;

public class HttpClientProducer {

    @Produces
    @ApplicationScoped
    public HttpClient createHttpClient() {
        return HttpClient.newHttpClient();
    }
}
