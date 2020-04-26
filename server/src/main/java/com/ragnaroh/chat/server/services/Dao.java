package com.ragnaroh.chat.server.services;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.ragnaroh.chat.server.common.MapBuilder;

public class Dao {

   @Autowired
   private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

   protected MapBuilder<String, Object> paramsBuilder() {
      return new MapBuilder<>();
   }

   protected <T> T queryForSingleResultOrNull(String sql, Map<String, ?> params, RowMapper<T> rowMapper) {
      return namedParameterJdbcTemplate.query(sql, params, new SingleResultOrNullExtractor<>(rowMapper));
   }

   protected <T> T queryForSingleResult(String sql, Map<String, ?> params, RowMapper<T> rowMapper) {
      return namedParameterJdbcTemplate.query(sql, params, new SingleResultExtractor<>(rowMapper));
   }

   protected <T> List<T> query(String sql, Map<String, ?> params, RowMapper<T> rowMapper) {
      return namedParameterJdbcTemplate.query(sql, params, (rs, i) -> rowMapper.mapRow(rs));
   }

   protected Integer queryForInteger(String sql, Map<String, ?> params) {
      return namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
   }

   protected Integer queryForNullableInteger(String sql, Map<String, ?> params) {
      return queryForSingleResultOrNull(sql, params, rs -> rs.getInt(1));
   }

   protected String queryForString(String sql, Map<String, ?> params) {
      return namedParameterJdbcTemplate.queryForObject(sql, params, String.class);
   }

   protected String queryForNullableString(String sql, Map<String, ?> params) {
      return queryForSingleResultOrNull(sql, params, rs -> rs.getString(1));
   }

   protected List<String> queryForStringList(String sql, Map<String, ?> params) {
      return namedParameterJdbcTemplate.queryForList(sql, params, String.class);
   }

   protected int update(String sql, Map<String, ?> params) {
      return namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(params));
   }

   protected int updateAndReturnId(String sql, Map<String, ?> params) {
      KeyHolder keyHolder = new GeneratedKeyHolder();
      namedParameterJdbcTemplate.update(sql, new MapSqlParameterSource(params), keyHolder);
      Object key = keyHolder.getKey();
      if (key == null) {
         throw new IllegalArgumentException("No key generated.");
      }
      return ((Number) key).intValue();
   }

   private static final class SingleResultOrNullExtractor<T> implements ResultSetExtractor<T> {

      private final RowMapper<T> rowMapper;

      private SingleResultOrNullExtractor(RowMapper<T> rowMapper) {
         this.rowMapper = rowMapper;
      }

      public T extractData(ResultSet rs) throws SQLException {
         if (!rs.next()) {
            return null;
         }
         T data = rowMapper.mapRow(rs);
         if (rs.next()) {
            throw new IllegalStateException("Expected at most one entry, found at least two.");
         }
         return data;
      }
   }

   private static final class SingleResultExtractor<T> implements ResultSetExtractor<T> {

      private final RowMapper<T> rowMapper;

      private SingleResultExtractor(RowMapper<T> rowMapper) {
         this.rowMapper = rowMapper;
      }

      public T extractData(ResultSet rs) throws SQLException {
         if (!rs.next()) {
            throw new IllegalStateException("Expected an entry.");
         }
         T data = rowMapper.mapRow(rs);
         if (rs.next()) {
            throw new IllegalStateException("Expected only one entry, found at least two.");
         }
         return data;
      }
   }

   protected interface RowMapper<T> {

      T mapRow(ResultSet rs) throws SQLException;

   }

}
