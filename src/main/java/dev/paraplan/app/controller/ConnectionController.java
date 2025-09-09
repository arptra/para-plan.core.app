package dev.paraplan.app.controller;

import dev.paraplan.app.model.ConnectionInfo;
import dev.paraplan.app.model.ConnectionRequest;
import dev.paraplan.app.service.ConnectionRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/connections")
public class ConnectionController {
    private final ConnectionRegistry registry;

    public ConnectionController(ConnectionRegistry registry) {
        this.registry = registry;
    }

    @PostMapping
    public Map<String, String> create(@RequestBody ConnectionRequest req) {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", req.host(), req.port(), req.database());
        String id = registry.create(jdbcUrl, req.username(), req.password(), req.info());
        return Map.of("id", id);
    }

    @GetMapping
    public List<ConnectionInfo> list() {
        return registry.list();
    }
}
