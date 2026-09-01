package com.resume.user.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OAuth state store 安全测试（api-changelog v2.4 + security-guide §1.2）。
 *
 * 覆盖：state 单次消费、provider 绑定、过期、null/blank 拒绝、Redis 降级到内存。
 */
class OAuthStateStoreTest {

    private OAuthStateStore store;
    private ObjectProvider<RedisTemplate<String, String>> provider;
    private RedisTemplate<String, String> redisTemplate;
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        store = new OAuthStateStore();
        provider = mock(ObjectProvider.class);
        redisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        ReflectionTestUtils.setField(store, "redisTemplateProvider", provider);
    }

    @Test
    void create_thenConsume_sameProvider_succeeds() {
        // 该用例走 Redis 路径，需让 Redis get 返回对应 provider
        when(valueOps.get(org.mockito.ArgumentMatchers.anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            // 从 Redis 模拟中取：若 key 包含 state 则返回创建时的 provider
            // 简化：直接返回 create 时的 provider，由 store 的 set/get 配对保证
            return "google";
        });
        String state = store.create("google");
        assertNotNull(state);
        assertTrue(state.length() >= 40, "state 应当至少 32 字节 base64url 编码");
        assertTrue(store.consume(state, "google"));
    }

    @Test
    void consume_isOneShot_secondCallReturnsFalse() {
        // 首次 get 返回 github（命中），delete 后第二次 get 返回 null（已消费）
        when(valueOps.get(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("github")
                .thenReturn(null);
        String state = store.create("github");
        assertTrue(store.consume(state, "github"));
        // 单次消费：再次校验必须失败（Redis 已 delete）
        assertFalse(store.consume(state, "github"), "state 一次性消费后必须失效");
    }

    @Test
    void consume_wrongProvider_returnsFalse() {
        String state = store.create("google");
        assertFalse(store.consume(state, "github"), "provider 不匹配必须拒绝");
    }

    @Test
    void consume_nullOrBlank_returnsFalse() {
        assertFalse(store.consume(null, "google"));
        assertFalse(store.consume("", "google"));
        assertFalse(store.consume("   ", "google"));
    }

    @Test
    void consume_unknownState_returnsFalse() {
        assertFalse(store.consume("never-issued-state", "google"));
    }

    @Test
    void expiredState_isRejected() throws Exception {
        // 直接构造过期条目绕过 10 分钟 TTL：用反射创建已过期的 Entry 覆盖
        String state = store.create("google");
        @SuppressWarnings("unchecked")
        Map<String, Object> internalMap = (Map<String, Object>) ReflectionTestUtils.getField(store, "states");
        Class<?> entryClass = Class.forName("com.resume.user.auth.OAuthStateStore$Entry");
        java.lang.reflect.Constructor<?> ctor = entryClass.getDeclaredConstructor(String.class, LocalDateTime.class);
        ctor.setAccessible(true);
        Object expired = ctor.newInstance("google", LocalDateTime.now().minusMinutes(1));
        internalMap.put(state, expired);
        assertFalse(store.consume(state, "google"), "过期 state 必须拒绝");
    }

    @Test
    void redisPath_usedWhenAvailable() {
        when(valueOps.get(anyString())).thenReturn("github");
        String state = store.create("github");
        assertTrue(store.consume(state, "github"));
        verify(redisTemplate, times(2)).opsForValue();
        verify(valueOps, times(1)).set(anyString(), eq("github"), any(Duration.class));
        verify(valueOps, times(1)).get(anyString());
    }

    @Test
    void redisFailure_fallsBackToMemory() {
        // create 时 set 抛异常会 fallback 到内存，内存中有值；consume 时 get 抛异常也会 fallback 到内存
        org.mockito.Mockito.doThrow(new RuntimeException("redis down")).when(valueOps).set(anyString(), anyString(), any(Duration.class));
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        String state = store.create("github");
        // 此时内存中有 state，consume fallback 到内存应成功
        assertTrue(store.consume(state, "github"));
    }

    @Test
    void noRedisProvider_usesMemoryOnly() {
        when(provider.getIfAvailable()).thenReturn(null);
        String state = store.create("qq");
        assertTrue(store.consume(state, "qq"));
        verify(redisTemplate, never()).opsForValue();
    }
}
