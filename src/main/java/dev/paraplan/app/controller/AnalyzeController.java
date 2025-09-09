package dev.paraplan.app.controller;

import dev.paraplan.app.model.*;
import dev.paraplan.app.service.*;
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
  private final LockAdvisor lockAdvisor;
  private final ServerFitService serverFitService;
  private final NPlusOneDetector nPlusOneDetector;

  public AnalyzeController(ExplainService explainService,
                           CostPredictor costPredictor,
                           LandscapeService landscapeService,
                           ProbeService probeService,
                           MonteCarloService monteCarloService,
                           RecommendationService recommendationService,
                           AdviceService adviceService,
                           LockAdvisor lockAdvisor,
                           ServerFitService serverFitService,
                           NPlusOneDetector nPlusOneDetector) {
    this.explainService = explainService;
    this.costPredictor = costPredictor;
    this.landscapeService = landscapeService;
    this.probeService = probeService;
    this.monteCarloService = monteCarloService;
    this.recommendationService = recommendationService;
    this.adviceService = adviceService;
    this.lockAdvisor = lockAdvisor;
    this.serverFitService = serverFitService;
    this.nPlusOneDetector = nPlusOneDetector;
  }

  @PostMapping("/analyze")
  public AnalyzeResponse analyze(@RequestBody AnalyzeRequest req) throws Exception {
    String sql = req.sql();
    String connectionId = req.connectionId();
    String schema = req.schema();
    String json = explainService.explainJson(connectionId, schema, sql);
    PlanFeatures features = explainService.parse(json, sql);
    PredictedMetrics predicted = costPredictor.predict(connectionId, schema, sql, features);
    LandscapeReport landscape = landscapeService.scan(connectionId, schema, sql);
    SelectivityReport selectivity = probeService.probe(connectionId, schema, sql);
    int samples = req.options() != null && req.options().mcSamples() != null ? req.options().mcSamples() : 25;
    Distribution distribution = monteCarloService.simulate(sql, samples);

    var locks = lockAdvisor.analyze(sql, predicted);
    var serverFit = serverFitService.estimate(features);
    var nplus1 = nPlusOneDetector.detect(sql);

    List<Recommendation> recs = recommendationService.build(features, predicted);
    if (!nplus1.isEmpty()) {
      recs.add(new Recommendation(
          "REWRITE",
          "Избавиться от N+1",
          nplus1.get(0),
          "-- пример: SELECT p.id, count(c.id) FROM parent p LEFT JOIN child c ON c.parent_id=p.id GROUP BY p.id;",
          8,
          "med"));
    }

    var draft = new AdviceService.AnalyzeResponseDraft(
        features, predicted, recs, locks, serverFit, nplus1
    );
    List<String> advice = adviceService.buildAdvice(draft);

    return new AnalyzeResponse(
        features,
        predicted,
        landscape,
        selectivity,
        distribution,
        locks,
        serverFit,
        nplus1,
        recs,
        advice
    );
  }
}
