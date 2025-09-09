package dev.paraplan.app.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.paraplan.app.model.ConnectionInfo;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectionRegistry {
    private final Map<String, HikariDataSource> sources = new ConcurrentHashMap<>();
    private final Map<String, ConnectionInfo> infos = new ConcurrentHashMap<>();

    public String create(String url, String username, String password, String info) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(username);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(4);
        cfg.setMinimumIdle(0);
        HikariDataSource ds = new HikariDataSource(cfg);
        String id = UUID.randomUUID().toString();
        sources.put(id, ds);
        infos.put(id, new ConnectionInfo(id, url, username, info));
        return id;
    }

    public List<ConnectionInfo> list() {
        return new ArrayList<>(infos.values());
    }

    public DataSource get(String id) {
        DataSource ds = sources.get(id);
        if (ds == null) throw new IllegalArgumentException("Unknown connection id: " + id);
        return ds;
    }

    public Connection getConnection(String id, String schema) throws SQLException {
        Connection c = get(id).getConnection();
        if (schema != null && !schema.isBlank()) {
            try (Statement st = c.createStatement()) {
                String safe = schema.replace("\"", "\"\"");
                st.execute("SET search_path TO \"" + safe + "\"");
            }
        }
        return c;
    }
}
