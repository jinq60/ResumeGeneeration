package com.resume.user.auth.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GitHub /user/emails 响应解析安全测试：
 * 仅接受 primary==true && verified==true 的邮箱，防止未验证邮箱账号接管。
 */
class GithubAuthProviderEmailTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String parse(String json) throws Exception {
        JsonNode array = objectMapper.readTree(json);
        return GithubAuthProvider.extractVerifiedPrimaryEmail(array);
    }

    @Test
    void shouldAcceptVerifiedPrimaryEmail() throws Exception {
        String json = """
                [
                  {"email":"other@example.com","primary":false,"verified":true},
                  {"email":"me@example.com","primary":true,"verified":true}
                ]
                """;
        assertEquals("me@example.com", parse(json));
    }

    @Test
    void shouldRejectUnverifiedPrimaryEmail() throws Exception {
        // 攻击者将受害者未验证邮箱设为主邮箱：不得采纳
        String json = """
                [
                  {"email":"victim@example.com","primary":true,"verified":false},
                  {"email":"attacker@example.com","primary":false,"verified":true}
                ]
                """;
        assertNull(parse(json));
    }

    @Test
    void shouldNotFallbackToFirstElementWhenNoPrimaryVerified() throws Exception {
        // 无 primary+verified 条目时禁止取第一条的兜底行为
        String json = """
                [
                  {"email":"a@example.com","primary":false,"verified":true},
                  {"email":"b@example.com","primary":true,"verified":null}
                ]
                """;
        assertNull(parse(json));
    }

    @Test
    void shouldReturnNullForEmptyOrInvalidPayload() throws Exception {
        assertNull(parse("[]"));
        assertNull(parse("{\"email\":\"x@example.com\"}"));
    }
}
