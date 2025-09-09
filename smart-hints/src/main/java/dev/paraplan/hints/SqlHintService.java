package dev.paraplan.hints;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/**
 * Analyzes SQL queries and returns hints pointing to problematic fragments.
 */
@Service
public class SqlHintService {

    private static final Pattern SELECT_STAR =
            Pattern.compile("select\\s+\\*", Pattern.CASE_INSENSITIVE);

    private static final Pattern LEADING_WILDCARD_LIKE =
            Pattern.compile("like\\s+'%[^']*'", Pattern.CASE_INSENSITIVE);

    /**
     * Parse SQL and produce a list of hints with positions and suggestions.
     * <p>
     * The list of recommendations normally comes from the <code>/api/analyze</code> endpoint.
     */
    public List<SqlHint> analyze(String sql, List<RecommendationDto> recs) {
        var hints = new ArrayList<SqlHint>();

        // 1) generic lint rules
        Matcher m = SELECT_STAR.matcher(sql);
        if (m.find()) {
            int star = sql.indexOf('*', m.start());
            hints.add(new SqlHint(
                    star,
                    star + 1,
                    "Avoid SELECT * to reduce scanned data",
                    "SELECT column1, column2"));
        }

        Matcher like = LEADING_WILDCARD_LIKE.matcher(sql);
        while (like.find()) {
            hints.add(new SqlHint(
                    like.start(),
                    like.end(),
                    "Leading wildcard in LIKE prevents index usage",
                    "LIKE 'value%'"));
        }

        // 2) correlate with AnalyzeResponse recommendations
        for (RecommendationDto rec : recs) {
            String kind = rec.kind() == null ? "" : rec.kind().toUpperCase();
            switch (kind) {
                case "INDEX" -> addIndexHint(sql, rec, hints);
                case "STATS" -> addStatsHint(sql, rec, hints);
                case "CONFIG" -> hints.add(new SqlHint(0, sql.length(), rec.title(), rec.example()));
                case "REWRITE", "SQL" -> addRewriteHint(sql, rec, hints);
                default -> { /* ignore */ }
            }
        }

        return hints;
    }

    /** Backward-compatible overload. */
    public List<SqlHint> analyze(String sql) {
        return analyze(sql, List.of());
    }

    private static void addIndexHint(String sql, RecommendationDto rec, List<SqlHint> hints) {
        Matcher ex = Pattern.compile("ON\\s+(\\w+)\\s*\\((\\w+)\\)", Pattern.CASE_INSENSITIVE)
                .matcher(rec.example());
        if (ex.find()) {
            String column = ex.group(2);
            int idx = indexOfIgnoreCase(sql, column);
            if (idx >= 0) {
                hints.add(new SqlHint(idx, idx + column.length(), rec.title(), rec.example()));
            }
        }
    }

    private static void addStatsHint(String sql, RecommendationDto rec, List<SqlHint> hints) {
        Matcher ex = Pattern.compile("ANALYZE\\s+(\\w+)", Pattern.CASE_INSENSITIVE)
                .matcher(rec.example());
        if (ex.find()) {
            String table = ex.group(1);
            int idx = indexOfIgnoreCase(sql, table);
            if (idx >= 0) {
                hints.add(new SqlHint(idx, idx + table.length(), rec.title(), rec.example()));
            }
        }
    }

    private static void addRewriteHint(String sql, RecommendationDto rec, List<SqlHint> hints) {
        String snippet = rec.example();
        int idx = indexOfIgnoreCase(sql, snippet);
        if (idx >= 0) {
            hints.add(new SqlHint(idx, idx + snippet.length(), rec.title(), rec.example()));
        }
    }

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase().indexOf(needle.toLowerCase());
    }
}
