package dev.paraplan.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ConnectionRequest(
        String host,
        int port,
        String database,
        @JsonProperty("user") String username,
        String password,
        String info
) {}
