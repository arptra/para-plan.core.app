package dev.paraplan.app.controller;

import dev.paraplan.app.model.CreateConnectionRequest;
import dev.paraplan.app.model.CreateConnectionResponse;
import dev.paraplan.app.service.ConnectionManager;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/connection")
public class ConnectionController {
    private final ConnectionManager connections;

    public ConnectionController(ConnectionManager connections) {
        this.connections = connections;
    }

    @GetMapping
    public Collection<String> list() {
        return connections.listIds();
    }

    @PostMapping
    public CreateConnectionResponse create(@RequestBody CreateConnectionRequest req) {
        String id = connections.create(req.host(), req.port(), req.database(), req.user(), req.password());
        return new CreateConnectionResponse(id);
    }
}
