package com.gurskiyy.client;

import com.gurskiyy.exception.ProxyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpProxyClientTest {

    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private HttpResponse<byte[]> mockHttpResponse;

    private HttpProxyClient httpProxyClient;

    @BeforeEach
    void setUp() {
        httpProxyClient = new HttpProxyClient(mockHttpClient);
    }

    @Test
    void shouldReturnSuccessfulResponse() throws Exception {
        byte[] expectedBody = "Hello World".getBytes();
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(expectedBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        URI requestUri = URI.create("https://example.com");
        HttpResponse<byte[]> response = httpProxyClient.sendRequest(requestUri);

        assertEquals(200, response.statusCode());
        assertArrayEquals(expectedBody, response.body());
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldThrowProxyExceptionOnNetworkFailure() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Network error"));

        URI requestUri = URI.create("https://example.com");

        ProxyException exception = assertThrows(ProxyException.class, () ->
                httpProxyClient.sendRequest(requestUri)
        );

        assertTrue(exception.getMessage().contains("Error forwarding request"));
    }

    @Test
    void shouldHandleMultipleChoicesResponse() throws Exception {
        byte[] expectedBody = "Multiple Choices".getBytes();

        when(mockHttpResponse.statusCode()).thenReturn(300);
        when(mockHttpResponse.body()).thenReturn(expectedBody);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        URI requestUri = URI.create("https://example.com");

        HttpResponse<byte[]> response = httpProxyClient.sendRequest(requestUri);

        assertEquals(300, response.statusCode());
        assertArrayEquals(expectedBody, response.body());
    }

    @Test
    void shouldHandleRedirectResponse() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(302);
        when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(Map.of("Location", List.of("https://redirected.com")), (k, v) -> true));
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        URI requestUri = URI.create("https://example.com");

        HttpResponse<byte[]> response = httpProxyClient.sendRequest(requestUri);

        assertEquals(302, response.statusCode());
        assertEquals("https://redirected.com", response.headers().firstValue("Location").orElse(""));
    }

    @Test
    void shouldHandleClientErrorResponse() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(404);
        when(mockHttpResponse.body()).thenReturn("Not Found".getBytes());
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        URI requestUri = URI.create("https://example.com");

        HttpResponse<byte[]> response = httpProxyClient.sendRequest(requestUri);

        assertEquals(404, response.statusCode());
        assertEquals("Not Found", new String(response.body()));
    }

    @Test
    void shouldHandleServerErrorResponse() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(500);
        when(mockHttpResponse.body()).thenReturn("Server Error".getBytes());
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        URI requestUri = URI.create("https://example.com");

        HttpResponse<byte[]> response = httpProxyClient.sendRequest(requestUri);

        assertEquals(500, response.statusCode());
        assertEquals("Server Error", new String(response.body()));
    }

    @Test
    void shouldHandleInterruptedException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Request was interrupted"));

        URI requestUri = URI.create("https://example.com");

        ProxyException exception = assertThrows(ProxyException.class, () ->
                httpProxyClient.sendRequest(requestUri)
        );

        assertTrue(exception.getMessage().contains("Interrupted: Request was interrupted"));
        assertTrue(Thread.currentThread().isInterrupted());
    }
}
