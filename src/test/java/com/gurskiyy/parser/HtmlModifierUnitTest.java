package com.gurskiyy.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

class HtmlModifierUnitTest {

    private static final String PROXY_BASE_URI = "http://localhost:8080/?target=https://quarkus.io";
    private static final String TARGET_DOMAIN = "https://quarkus.io";
    private static final HtmlModifier HTML_MODIFIER = new HtmlModifier();

    @Test
    void givenText_whenProcessText_thenTrademarkAppended() {
        assertProcessedText("Sample text with random words.", "Sample™ text with random™ words.");
    }

    @Test
    void givenAbsoluteLink_whenModifyHtml_thenLinkRewritten() {
        String html = """
                <html>
                    <body>
                        <a href="https://pt.quarkus.io/some/page">Link</a>
                    </body>
                </html>
                """;

        assertHtmlLinkRewritten(html, "https://pt.quarkus.io", "/some/page");
    }

    @Test
    void givenRelativeLink_whenModifyHtml_thenLinkRewritten() {
        String html = """
                <html>
                    <body>
                        <a href="/docs">Docs</a>
                    </body>
                </html>
                """;

        assertHtmlLinkRewritten(html, TARGET_DOMAIN, "/docs");
    }

    @Test
    void givenHtmlWithMixedLinks_whenModifyHtml_thenAllLinksRewritten() {
        String html = """
                <html>
                    <body>
                        <a href="https://pt.quarkus.io/some/page">Absolute</a>
                        <a href="/help">Relative</a>
                    </body>
                </html>
                """;

        String modifiedHtml = HTML_MODIFIER.modifyHtml(html, PROXY_BASE_URI, "https://pt.quarkus.io");

        assertTrue(modifiedHtml.contains(expectedHref("https://pt.quarkus.io", "/some/page")));
        assertTrue(modifiedHtml.contains(expectedHref("https://pt.quarkus.io", "/help")));
    }

    private void assertProcessedText(String input, String expected) {
        assertEquals(expected, HTML_MODIFIER.processText(input));
    }

    private void assertHtmlLinkRewritten(String html, String targetDomain, String path) {
        String modifiedHtml = HTML_MODIFIER.modifyHtml(html, PROXY_BASE_URI, targetDomain);
        assertTrue(modifiedHtml.contains(expectedHref(targetDomain, path)));
    }

    private String expectedHref(String domain, String path) {
        String encodedDomain = URLEncoder.encode(domain, StandardCharsets.UTF_8);
        return "href=\"http://localhost:8080" + path + "?target=" + encodedDomain;
    }
}
