package dev.paraplan.app.controller;

import dev.paraplan.app.model.*;
import dev.paraplan.app.service.AdviceService;
import dev.paraplan.app.service.CostPredictor;
import dev.paraplan.app.service.ExplainService;
import dev.paraplan.app.service.LandscapeService;
import dev.paraplan.app.service.MonteCarloService;
import dev.paraplan.app.service.ProbeService;
import dev.paraplan.app.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

  private final ExplainService explainService;
  private final CostPredictor costPredictor;
  private final LandscapeService landscapeService;
  private final ProbeService probeService;
  private final MonteCarloService monteCarloService;
  private final RecommendationService recommendationService;
  private final AdviceService adviceService;

  public AnalyzeController(ExplainService explainService,
                           CostPredictor costPredictor,
                           LandscapeService landscapeService,
                           ProbeService probeService,
                           MonteCarloService monteCarloService,
                           RecommendationService recommendationService,
                           AdviceService adviceService) {
    this.explainService = explainService;
    this.costPredictor = costPredictor;
    this.landscapeService = landscapeService;
    this.probeService = probeService;
    this.monteCarloService = monteCarloService;
    this.recommendationService = recommendationService;
    this.adviceService = adviceService;
  }

  @PostMapping("/analyze")
  public AnalyzeResponse analyze(@RequestBody AnalyzeRequest req) throws Exception {
    String connId = req.connectionId();
    String schema = req.schema();
    String sql = req.sql();
    String json = explainService.explainJson(connId, schema, sql);
    PlanFeatures features = explainService.parse(json, sql);
    PredictedMetrics predicted = costPredictor.predict(features);
    LandscapeReport landscape = landscapeService.scan(connId, schema, sql);
    SelectivityReport selectivity = probeService.probe(connId, schema, sql);
    int samples = req.options() != null && req.options().mcSamples() != null ? req.options().mcSamples() : 25;
    Distribution distribution = monteCarloService.simulate(connId, schema, sql, samples);

    List<Recommendation> recs = recommendationService.build(features, predicted);

    var draft = new AdviceService.AnalyzeResponseDraft(
        features, predicted, recs
    );
    List<String> advice = adviceService.buildAdvice(draft);

    return new AnalyzeResponse(
        features,
        predicted,
        landscape,
        selectivity,
        distribution,
        recs,
        advice
    );
  }
}
