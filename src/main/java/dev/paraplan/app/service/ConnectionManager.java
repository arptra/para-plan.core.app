package dev.paraplan.app.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectionManager {
    private final Map<String, DataSource> connections = new ConcurrentHashMap<>();

    public String create(String host, int port, String database, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(4);
        cfg.setMinimumIdle(0);
        HikariDataSource ds = new HikariDataSource(cfg);
        String id = UUID.randomUUID().toString();
        connections.put(id, ds);
        return id;
    }

    public Collection<String> listIds() {
        return connections.keySet();
    }

    public Connection getConnection(String id, String schema) throws SQLException {
        DataSource ds = connections.get(id);
        if (ds == null) {
            throw new IllegalArgumentException("Unknown connection id: " + id);
        }
        Connection c = ds.getConnection();
        if (schema != null && !schema.isEmpty()) {
            try (Statement st = c.createStatement()) {
                st.execute("SET search_path TO " + schema);
            }
        }
        return c;
    }
}

