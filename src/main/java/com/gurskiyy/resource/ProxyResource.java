package com.gurskiyy.resource;


import java.net.URI;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import com.gurskiyy.parser.HtmlModifier;
import com.gurskiyy.exception.ProxyException;
import com.gurskiyy.client.HttpProxyClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.*;

@Path("/{path:.*}")
public class ProxyResource {

    private static final String DEFAULT_TARGET = "https://quarkus.io";

    private final HtmlModifier htmlModifier;
    private final HttpProxyClient httpProxyClient;

    @Inject
    public ProxyResource(HtmlModifier htmlModifier, HttpProxyClient httpProxyClient) {
        this.htmlModifier = htmlModifier;
        this.httpProxyClient = httpProxyClient;
    }

    @GET
    public Response proxyGet(@QueryParam("target") String targetDomain, @Context UriInfo uriInfo) {
        final String target = (targetDomain == null || targetDomain.isEmpty()) ? DEFAULT_TARGET : targetDomain;
        URI targetUri = buildTargetUri(uriInfo, target);
        HttpResponse<byte[]> response = httpProxyClient.sendRequest(targetUri);
        return processResponse(response, uriInfo, target);
    }

    private Response processResponse(HttpResponse<byte[]> response, UriInfo uriInfo, String target) {
        int statusCode = response.statusCode();
        return isRedirect(statusCode) ? handleRedirect(response, uriInfo, target, statusCode)
                : handleContent(response, uriInfo, target);
    }

    private boolean isRedirect(int statusCode) {
        return statusCode >= 300 && statusCode < 400;
    }

    private Response handleRedirect(HttpResponse<byte[]> response, UriInfo uriInfo, String target, int statusCode) {
        String originalLocation = response.headers().firstValue("Location").orElse("");
        if (!originalLocation.isEmpty() && originalLocation.startsWith(target)) {
            String rewrittenLocation = rewriteRedirect(originalLocation, uriInfo.getBaseUri().toString(), target);
            return Response.status(statusCode).header("Location", rewrittenLocation).build();
        }
        return Response.status(statusCode).build();
    }

    private Response handleContent(HttpResponse<byte[]> response, UriInfo uriInfo, String target) {
        String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        byte[] body = response.body();

        if (contentType.contains("text/html")) {
            String html = new String(body, StandardCharsets.UTF_8);
            String proxyUriWithTarget = uriInfo.getBaseUri().toString() + "?target=" + target;
            String modifiedHtml = htmlModifier.modifyHtml(html, proxyUriWithTarget, target);
            return Response.ok(modifiedHtml, contentType).build();
        }
        return Response.ok(body, contentType).build();
    }

    private URI buildTargetUri(UriInfo info, String target) {
        try {
            UriBuilder builder = UriBuilder.fromUri(target);
            if (!info.getPath().isEmpty()) {
                builder.path(info.getPath());
            }

            MultivaluedMap<String, String> queryParams = info.getQueryParameters();
            queryParams.forEach((key, values) -> {
                if (!"target".equals(key)) {
                    values.forEach(value -> builder.queryParam(key, value));
                }
            });

            return builder.build();
        } catch (Exception e) {
            throw new ProxyException("Error building target URI", e);
        }
    }

    private String rewriteRedirect(String originalLocation, String proxyBaseUri, String target) {
        return originalLocation.replaceFirst(Pattern.quote(target), removeTrailingSlash(proxyBaseUri)) +
                (originalLocation.contains("?") ? "&" : "?") + "target=" + target;
    }

    private String removeTrailingSlash(String uri) {
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }
}
