package dev.paraplan.app.controller;

import dev.paraplan.hints.RecommendationDto;
import dev.paraplan.hints.SqlHint;
import dev.paraplan.hints.SqlHintService;
import dev.paraplan.app.model.AnalyzeRequest;
import dev.paraplan.app.model.AnalyzeResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SqlHintController {

    private final SqlHintService service;
    private final AnalyzeController analyzeController;

    public SqlHintController(SqlHintService service, AnalyzeController analyzeController) {
        this.service = service;
        this.analyzeController = analyzeController;
    }

    @PostMapping(value = "/sql-hints", consumes = MediaType.APPLICATION_JSON_VALUE)
    public List<SqlHint> hints(@RequestBody SqlRequest req) throws Exception {
        if (req.sql() == null || req.sql().isBlank()) {
            throw new IllegalArgumentException("SQL must not be empty");
        }
        AnalyzeResponse resp = analyzeController.analyze(new AnalyzeRequest(req.sql(), req.connectionId(), req.schema(), null));
        var recs = resp.recommendations().stream()
                .map(r -> new RecommendationDto(r.kind(), r.title(), r.example()))
                .toList();
        return service.analyze(req.sql(), recs);
    }

    public record SqlRequest(String sql, String connectionId, String schema) {}
}
