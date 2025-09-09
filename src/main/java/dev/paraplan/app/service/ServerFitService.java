package dev.paraplan.app.service;

import dev.paraplan.app.model.PlanFeatures;
import dev.paraplan.app.model.ServerFit;
import org.springframework.stereotype.Service;

@Service
public class ServerFitService {
    public ServerFit estimate(PlanFeatures f) {
        if (f == null) return new ServerFit("N/A","N/A","N/A");
        long rows = f.planRows();
        String workMem = rows > 1_000_000 ? "128-512MB" : rows > 100_000 ? "32-128MB" : "4-32MB";
        String sharedBuffers = rows > 10_000_000 ? "25-40% RAM" : rows > 1_000_000 ? "15-25% RAM" : "10-15% RAM";
        String effective = rows > 10_000_000 ? "70-80% RAM" : "50-70% RAM";
        return new ServerFit(workMem, sharedBuffers, effective);
    }
}
