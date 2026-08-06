package com.resume.common.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;

/**
 * Shared allowlist for rich text persisted in resume sections.
 */
public final class RichTextSanitizer {

    private static final Safelist ALLOWED_TAGS = Safelist.none()
            .addTags("p", "br", "strong", "b", "em", "i", "u", "ul", "ol", "li", "a")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https");

    private RichTextSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        Document.OutputSettings settings = new Document.OutputSettings()
                .prettyPrint(false)
                .syntax(Document.OutputSettings.Syntax.xml);
        return Jsoup.clean(html, "", ALLOWED_TAGS, settings);
    }

    public static String toPlainText(String html) {
        String sanitized = sanitize(html);
        if (sanitized.isBlank()) {
            return "";
        }
        var body = Jsoup.parseBodyFragment(sanitized).body();
        body.select("br").forEach(node -> node.replaceWith(new TextNode("\n")));
        body.select("p, li").forEach(node -> node.appendChild(new TextNode("\n")));
        return body.wholeText()
                .replace('\u00a0', ' ')
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
