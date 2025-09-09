package dev.paraplan.app.service;

import dev.paraplan.app.util.SqlUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NPlusOneDetector {
    public List<String> detect(String sql) {
        int c = SqlUtil.countCorrelatedSubqueries(sql);
        if (c > 1) {
            return List.of("Обнаружено " + c + " коррелированных подзапросов; перепишите через JOIN + GROUP BY");
        }
        return List.of();
    }
}
