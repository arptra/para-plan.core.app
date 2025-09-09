package dev.paraplan.app.model;

public record CreateConnectionRequest(String host, int port, String database, String user, String password) {}
