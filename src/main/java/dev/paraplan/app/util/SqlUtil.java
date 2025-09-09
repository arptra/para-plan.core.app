package dev.paraplan.app.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlUtil {
    private static final Pattern TABLE_PATTERN = Pattern.compile("(?i)\\b(from|join)\\s+([a-zA-Z0-9_.]+)");

    public static List<String> extractTableNames(String sql) {
        List<String> tables = new ArrayList<>();
        if (sql == null) return tables;
        Matcher m = TABLE_PATTERN.matcher(sql);
        while (m.find()) {
            String t = m.group(2);
            t = t.replaceAll("[^a-zA-Z0-9_.]", "");
            if (!t.isEmpty()) tables.add(t);
        }
        return tables;
    }

    public static int countCorrelatedSubqueries(String sql) {
        if (sql == null) return 0;
        Pattern p = Pattern.compile("(?i)\\(\\s*select[^)]*?where[^)]*?\\b([a-zA-Z0-9_]+)\\.([a-zA-Z0-9_]+)\\s*=\\s*\\1\\.[a-zA-Z0-9_]+" );
        Matcher m = p.matcher(sql);
        int count = 0;
        while (m.find()) count++;
        return count;
    }
}
