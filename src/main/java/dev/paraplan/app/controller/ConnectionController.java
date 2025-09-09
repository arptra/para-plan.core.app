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
        String id = registry.create(req.url(), req.username(), req.password());
        return Map.of("id", id);
    }

    @GetMapping
    public List<ConnectionInfo> list() {
        return registry.list();
    }
}
