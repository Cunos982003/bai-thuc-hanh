package re.edu.thuchanh.common;

import java.util.Locale;

public final class LikePattern {

    private LikePattern() {
    }

    public static String containing(String raw) {
        if (raw == null) {
            return null;
        }
        String escaped = raw
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped.toLowerCase(Locale.ROOT) + "%";
    }
}
