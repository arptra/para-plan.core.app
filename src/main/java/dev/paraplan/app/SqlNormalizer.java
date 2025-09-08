
package dev.paraplan.app;

import java.security.MessageDigest;
import java.util.HexFormat;

public class SqlNormalizer {
    public static Normalized normalize(String raw) {
        if (raw == null) raw = "";
        String s = raw.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("--.*?(\r?\n|$)", " ");
        s = s.replaceAll("\s+", " ").trim();
        s = s.replaceAll("(?i)'(?:''|[^'])*'", "?");
        s = s.replaceAll("(?i)\b\\d+(?:\\.\\d+)?\\b", "?");
        String fp = sha256(s.toLowerCase());
        return new Normalized(fp, s, raw);
    }
    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(s.getBytes()));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    public record Normalized(String fingerprint, String normalized, String sample) {}
}
