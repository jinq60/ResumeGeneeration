package com.resume.common.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RichTextSanitizerTest {

    @Test
    void sanitize_shouldKeepFormattingAndDropUnsafeMarkup() {
        String result = RichTextSanitizer.sanitize(
                "<p><strong>重点</strong></p><script>alert(1)</script>"
                        + "<a href='javascript:alert(2)'>危险</a>"
                        + "<a href='https://example.com' onclick='alert(3)'>安全</a>");

        assertAll(
                () -> assertTrue(result.contains("<strong>重点</strong>")),
                () -> assertFalse(result.contains("<script")),
                () -> assertFalse(result.contains("javascript:")),
                () -> assertFalse(result.contains("onclick")),
                () -> assertTrue(result.contains("https://example.com"))
        );
    }

    @Test
    void toPlainText_shouldReturnReadableContent() {
        String result = RichTextSanitizer.toPlainText("<p>第一段</p><ul><li>第二段</li></ul>");

        assertTrue(result.contains("第一段"));
        assertTrue(result.contains("第二段"));
    }
}
