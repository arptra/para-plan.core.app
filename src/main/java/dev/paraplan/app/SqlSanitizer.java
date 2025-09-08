
package dev.paraplan.app;

public final class SqlSanitizer {
  private SqlSanitizer() {}

  public static String clean(String raw) {
    if (raw == null) return "";
    String s = raw;

    // Replace smart quotes with ASCII
    s = s
        .replace('\u2018','\'')
        .replace('\u2019','\'')
        .replace('\u201C','"')
        .replace('\u201D','"');

    // Trim and drop trailing semicolon
    s = s.trim();
    if (s.endsWith(";")) s = s.substring(0, s.length()-1);

    // Disallow multiple statements
    int idx = s.indexOf(';');
    if (idx >= 0) {
      throw new IllegalArgumentException("Multiple SQL statements are not allowed.");
    }

    return s;
  }
}
