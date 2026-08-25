package com.resume.resume.handler;

import com.resume.resume.dto.SectionDTO;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SectionListTypeHandlerTest {

    private SectionListTypeHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SectionListTypeHandler();
    }

    @Test
    void parse_shouldReturnEmptyListForNullOrBlank() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("sections")).thenReturn(null);

        assertTrue(handler.getNullableResult(rs, "sections").isEmpty());
    }

    @Test
    void parse_shouldDeserializeValidJson() throws SQLException {
        String json = "[{\"id\":\"sec_1\",\"type\":\"profile\",\"title\":\"个人信息\","
                + "\"order\":0,\"visible\":true,\"data\":{}}]";
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("sections")).thenReturn(json);

        List<SectionDTO> sections = handler.getNullableResult(rs, "sections");

        assertEquals(1, sections.size());
        assertEquals("profile", sections.get(0).getType());
    }

    @Test
    void parse_shouldThrowWhenJsonCorrupted() throws SQLException {
        // 反序列化失败必须显式抛出，而非静默返回空列表（否则下次保存会把空数组写回 DB）
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("sections")).thenReturn("{not-a-valid-json");

        assertThrows(SQLException.class, () -> handler.getNullableResult(rs, "sections"));
    }
}
