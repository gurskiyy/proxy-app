package com.gurskiyy.resource;


import com.gurskiyy.client.HttpProxyClient;
import com.gurskiyy.parser.HtmlModifier;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyResourceUnitTest {

    @Mock
    private HttpProxyClient mockHttpProxyClient;
    @Mock
    private UriInfo mockUriInfo;
    @Mock
    private HttpResponse<byte[]> mockHttpResponse;

    private ProxyResource proxyResource;

    @BeforeEach
    void setUp() {
        proxyResource = new ProxyResource(new HtmlModifier(), mockHttpProxyClient);
        when(mockUriInfo.getPath()).thenReturn("");
        when(mockUriInfo.getBaseUri()).thenReturn(URI.create("http://localhost:8080/"));
        when(mockUriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
    }

    @Test
    void shouldRewriteRedirectCorrectly() {
        when(mockHttpResponse.statusCode()).thenReturn(302);
        when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Location", List.of("https://example.com/redirected")), (k, v) -> true)
        );
        when(mockHttpProxyClient.sendRequest(any())).thenReturn(mockHttpResponse);

        try (Response response = proxyResource.proxyGet("https://example.com", mockUriInfo)) {
            assertEquals(302, response.getStatus());
            assertEquals("http://localhost:8080/redirected?target=https://example.com", response.getHeaderString("Location"));
        }
    }

    @Test
    void shouldRewriteRedirectWithQueryParams() {
        when(mockHttpResponse.statusCode()).thenReturn(302);
        when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Location", List.of("https://example.com/page?param=value")), (k, v) -> true)
        );
        when(mockHttpProxyClient.sendRequest(any())).thenReturn(mockHttpResponse);

        try (Response response = proxyResource.proxyGet("https://example.com", mockUriInfo)) {
            assertEquals(302, response.getStatus());
            assertEquals("http://localhost:8080/page?param=value&target=https://example.com", response.getHeaderString("Location"));
        }
    }

    @Test
    void shouldHandleEmptyQueryParameters() {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("Valid response".getBytes());
        when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("text/html")), (k, v) -> true)
        );
        when(mockHttpProxyClient.sendRequest(any())).thenReturn(mockHttpResponse);

        try (Response response = proxyResource.proxyGet("https://example.com", mockUriInfo)) {
            assertEquals(200, response.getStatus());
            assertTrue(response.getEntity().toString().contains("Valid response"));
        }
    }

    @Test
    void shouldRemoveTrailingSlashCorrectly() {
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn("Test response".getBytes());
        when(mockHttpResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Type", List.of("text/html")), (k, v) -> true)
        );
        when(mockHttpProxyClient.sendRequest(any())).thenReturn(mockHttpResponse);

        try (Response response = proxyResource.proxyGet("https://example.com/", mockUriInfo)) {
            assertEquals(200, response.getStatus());
            assertTrue(response.getEntity().toString().contains("Test response"));
        }
    }
}
