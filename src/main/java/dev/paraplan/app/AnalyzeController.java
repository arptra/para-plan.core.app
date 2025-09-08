package dev.paraplan.app;

import dev.paraplan.app.model.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AnalyzeController {

  private final ExplainService explainService;
  private final RecommendationService recommendationService;
  private final AdviceService adviceService;

  public AnalyzeController(ExplainService explainService,
                           RecommendationService recommendationService,
                           AdviceService adviceService) {
    this.explainService = explainService;
    this.recommendationService = recommendationService;
    this.adviceService = adviceService;
  }

  @PostMapping("/analyze")
  public AnalyzeResponse analyze(@RequestBody AnalyzeRequest req) {
    ExplainSummary summary = explainService.summarize(req.sql(), req.options());

    List<Recommendation> recs = recommendationService.build(summary);

    var draft = new AdviceService.AnalyzeResponseDraft(
        summary.features(), summary.predicted(), recs
    );
    List<String> advice = adviceService.buildAdvice(draft);

    return new AnalyzeResponse(
        summary.features(),
        summary.predicted(),
        summary.landscape(),
        summary.selectivity(),
        summary.distribution(),
        recs,
        advice
    );
  }
}
