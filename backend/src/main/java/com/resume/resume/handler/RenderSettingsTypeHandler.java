package com.resume.resume.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.resume.dto.RenderSettings;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * resume.render_settings JSON 映射。
 */
@Slf4j
public class RenderSettingsTypeHandler extends BaseTypeHandler<RenderSettings> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RenderSettings parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            log.error("Failed to serialize render settings", e);
            throw new SQLException("Failed to serialize render settings", e);
        }
    }

    @Override
    public RenderSettings getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public RenderSettings getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public RenderSettings getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private RenderSettings parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, RenderSettings.class);
        } catch (Exception e) {
            log.error("Failed to deserialize render settings: {}", json, e);
            return null;
        }
    }
}
