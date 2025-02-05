package com.gurskiyy.parser;

import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@ApplicationScoped
public class HtmlModifier {

    private static final Pattern SIX_LETTER_WORD_PATTERN = Pattern.compile("\\b([A-Za-z]{6})\\b");
    private static final String TARGET_PARAM = "target";

    public String modifyHtml(String html, String proxyBaseUri, String targetDomain) {
        Document doc = Jsoup.parse(html, targetDomain);

        NodeTraversor.traverse(new NodeVisitor() {
            @Override
            public void head(Node node, int depth) {
                if (node instanceof TextNode textNode && isProcessable(node)) {
                    textNode.text(processText(textNode.getWholeText()));
                }
            }

            @Override
            public void tail(Node node, int depth) {
                //ignore
            }
        }, doc);

        for (Element link : doc.select("a[href]")) {
            String href = link.attr("href");
            if (href.startsWith("http://") || href.startsWith("https://")) {
                link.attr("href", rewriteAbsoluteUrl(href, proxyBaseUri));
            } else if (href.startsWith("/")) {
                link.attr("href", rewriteRelativeUrl(href, proxyBaseUri, targetDomain));
            }
        }
        return doc.outerHtml();
    }

    String processText(String text) {
        return SIX_LETTER_WORD_PATTERN.matcher(text).replaceAll("$1\u2122");
    }

    private boolean isProcessable(Node node) {
        return !(node.parent() instanceof Element element &&
                ("script".equalsIgnoreCase(element.nodeName()) || "style".equalsIgnoreCase(element.nodeName())));
    }

    private String rewriteAbsoluteUrl(String href, String proxyBaseUri) {
        try {
            URI uri = URI.create(href);
            String originalDomain = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1) {
                originalDomain += ":" + uri.getPort();
            }
            String path = uri.getPath();
            String query = uri.getQuery();
            String newHref = (path != null ? path : "") + (query != null && !query.isEmpty() ? "?" + query : "");
            String base = removeTrailingSlash(proxyBaseUri.split("\\?")[0]);
            String encodedOriginalDomain = URLEncoder.encode(originalDomain, StandardCharsets.UTF_8);
            String connector = newHref.contains("?") ? "&" : "?";
            return base + newHref + connector + TARGET_PARAM + "=" + encodedOriginalDomain;
        } catch (Exception e) {
            return href;
        }
    }

    private String rewriteRelativeUrl(String href, String proxyBaseUri, String targetDomain) {
        String base = removeTrailingSlash(proxyBaseUri.split("\\?")[0]);
        String encodedTarget = URLEncoder.encode(targetDomain, StandardCharsets.UTF_8);
        String connector = href.contains("?") ? "&" : "?";
        return base + href + connector + TARGET_PARAM + "=" + encodedTarget;
    }

    private String removeTrailingSlash(String uri) {
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }
}
