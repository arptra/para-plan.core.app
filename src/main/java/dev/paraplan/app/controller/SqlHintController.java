package dev.paraplan.app.controller;

import dev.paraplan.hints.RecommendationDto;
import dev.paraplan.hints.SqlHint;
import dev.paraplan.hints.SqlHintService;
import dev.paraplan.app.controller.AnalyzeController;
import dev.paraplan.app.model.AnalyzeRequest;
import dev.paraplan.app.model.AnalyzeResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SqlHintController {

    private final SqlHintService service;
    private final AnalyzeController analyzeController;
    private final ObjectMapper mapper;

    public SqlHintController(SqlHintService service, AnalyzeController analyzeController, ObjectMapper mapper) {
        this.service = service;
        this.analyzeController = analyzeController;
        this.mapper = mapper;
    }

    @PostMapping(value = "/sql-hints", consumes = MediaType.ALL_VALUE)
    public List<SqlHint> hints(@RequestBody byte[] body) throws Exception {
        String sql = extractSql(new String(body, StandardCharsets.UTF_8));
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL must not be empty");
        }
        AnalyzeResponse resp = analyzeController.analyze(new AnalyzeRequest(sql, null));
        var recs = resp.recommendations().stream()
                .map(r -> new RecommendationDto(r.kind(), r.title(), r.example()))
                .toList();
        return service.analyze(sql, recs);
    }

    private String extractSql(String body) throws JsonProcessingException {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.startsWith("{")) {
            SqlRequest req = mapper.readValue(trimmed, SqlRequest.class);
            return req.sql();
        }
        return trimmed;
    }

    public record SqlRequest(String sql) {}
}
