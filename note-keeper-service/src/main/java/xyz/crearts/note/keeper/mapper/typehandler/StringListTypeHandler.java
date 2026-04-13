package xyz.crearts.note.keeper.mapper.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class StringListTypeHandler extends BaseTypeHandler<List<String>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> TYPE_REF = new TypeReference<>() {};

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting List<String> to JSON", e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return fromJson(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return fromJson(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return fromJson(cs.getString(columnIndex));
    }

    private List<String> fromJson(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(json, TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting JSON to List<String>", e);
        }
    }
}
