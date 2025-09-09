package dev.paraplan.app.service;

import dev.paraplan.app.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdviceService {

  public List<String> buildAdvice(AnalyzeResponseDraft draft) {
    var adv = new ArrayList<String>();

    if (draft.features() != null) {
      if (draft.features().seqScans() > 0 && draft.features().planRows() > 0) {
        adv.add("⏱ Медленно из-за SeqScan по большой таблице и/или сортировки.");
      }
      if (draft.features().sortNodes() > 0 && draft.predicted() != null && draft.predicted().tempSpillRisk() >= 2) {
        adv.add("💾 Высокий риск temp spills (сортировки/хэш) — пролив во временные файлы.");
      }
    }
    if (draft.predicted() != null && draft.predicted().ioRisk() >= 2) {
      adv.add("📀 Высокий IO — читается много страниц с диска.");
    }
    if (draft.lockReport() != null && !"ACCESS SHARE".equals(draft.lockReport().level())) {
      adv.add("🔒 Уровень блокировки: " + draft.lockReport().level());
      if (!draft.lockReport().advice().isEmpty()) adv.add("🕒 " + String.join(". ", draft.lockReport().advice()));
    }
    if (draft.nPlusOneWarnings() != null && !draft.nPlusOneWarnings().isEmpty()) {
      adv.add("🐢 " + draft.nPlusOneWarnings().get(0));
    }
    if (draft.serverFit() != null) {
      adv.add("⚙️ work_mem " + draft.serverFit().workMem());
    }

    if (draft.recommendations() != null) {
      draft.recommendations().stream()
          .sorted((a,b) -> Integer.compare(b.impactScore(), a.impactScore()))
          .limit(3)
          .forEach(r -> {
            switch (r.kind()) {
              case "INDEX" -> adv.add("✅ Индекс: " + r.example());
              case "REWRITE" -> adv.add("✍️ Переписать запрос: " + r.example());
              case "STATS" -> adv.add("📊 Обновить статистику: " + r.example());
              case "CONFIG" -> adv.add("⚙️ Конфигурация: " + r.example());
              default -> adv.add("💡 " + r.title());
            }
          });
    }

    adv.add("🔁 Перезапусти анализ после изменений — p95 и план должны улучшиться.");
    adv.add("📝 Если в JSON внутри curl есть апострофы, удвой их: interval ''30 days''.");
    return adv;
  }

  public record AnalyzeResponseDraft(
      PlanFeatures features,
      PredictedMetrics predicted,
      List<Recommendation> recommendations,
      LockReport lockReport,
      ServerFit serverFit,
      List<String> nPlusOneWarnings
  ) {}
}
