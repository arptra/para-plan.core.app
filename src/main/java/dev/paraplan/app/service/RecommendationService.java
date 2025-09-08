package dev.paraplan.app.service;

import dev.paraplan.app.config.AdvisorProperties;
import dev.paraplan.app.model.AnalyzeResponse;
import dev.paraplan.app.model.PlanFeatures;
import dev.paraplan.app.model.PredictedMetrics;
import dev.paraplan.app.model.Recommendation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

  private final AdvisorProperties props;

  public RecommendationService(AdvisorProperties props) {
    this.props = props;
  }

  /** Основной вариант: принимаем весь AnalyzeResponse и строим рекомендации. */
  public List<Recommendation> build(AnalyzeResponse r) {
    if (r == null) return List.of();
    return build(r.features(), r.predicted());
  }

  /** Удобный перегруженный метод: если у тебя уже есть features/predicted отдельно. */
  public List<Recommendation> build(PlanFeatures features, PredictedMetrics predicted) {
    var out = new ArrayList<Recommendation>();
    if (features == null || predicted == null) return out;

    // 1) Большой SeqScan -> индекс
    if (features.seqScans() > 0
        && features.planRows() >= props.getThresholds().getSeqscanRowcount()) {
      out.add(new Recommendation(
          "INDEX",
          "Добавить индекс по условию запроса",
          "Большой SeqScan — оптимизатор не может эффективно отфильтровать данные",
          "/* пример */ CREATE INDEX idx_orders_created_at ON orders(created_at);",
          9,
          "low"
      ));
    }

    // 2) Риск пролива во временные файлы -> work_mem / индекс по сортировке
    if (predicted.tempSpillRisk() >= props.getThresholds().getTempSpillRisk()) {
      out.add(new Recommendation(
          "CONFIG",
          "Увеличить work_mem или убрать сортировку индексом",
          "Сортировка/хэш проливаются во временные файлы",
          "SET work_mem='64MB'; -- либо индекс под ORDER BY/WHERE",
          7,
          "low"
      ));
    }

    // 3) Высокий IO -> BRIN/BTREE + обслуживание/ANALYZE
    if (predicted.ioRisk() >= props.getThresholds().getIoRisk()) {
      out.add(new Recommendation(
          "INDEX",
          "Снизить IO (индексы/BRIN) + обслуживание",
          "Большой объём чтений с диска",
          "CREATE INDEX CONCURRENTLY idx_orders_created_brin ON orders USING brin(created_at);",
          7,
          "med"
      ));
      out.add(new Recommendation(
          "STATS",
          "Обновить статистику",
          "Неточная статистика ухудшает план",
          "ANALYZE orders;",
          5,
          "low"
      ));
    }

    return out;
  }
}
