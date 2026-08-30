package com.resume.ai.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.resume.ai.dto.ResumeOptimizeResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class SectionOptimizationListTypeHandler extends BaseTypeHandler<List<ResumeOptimizeResponse.SectionOptimization>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static final TypeReference<List<ResumeOptimizeResponse.SectionOptimization>> TYPE_REF = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<ResumeOptimizeResponse.SectionOptimization> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            log.error("Failed to serialize optimizations", e);
            throw new SQLException("Failed to serialize optimizations", e);
        }
    }

    @Override
    public List<ResumeOptimizeResponse.SectionOptimization> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<ResumeOptimizeResponse.SectionOptimization> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<ResumeOptimizeResponse.SectionOptimization> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private List<ResumeOptimizeResponse.SectionOptimization> parse(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (Exception e) {
            log.error("Failed to deserialize optimizations: {}", json, e);
            throw new SQLException("Failed to deserialize optimizations: " + e.getMessage(), e);
        }
    }
}
