package re.edu.thuchanh.common;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class LikePatternTest {

    @Test
    void nullStaysNull() {
        assertThat(LikePattern.containing(null)).isNull();
    }

    @Test
    void plainTextIsWrappedAndLowercased() {
        assertThat(LikePattern.containing("Backend")).isEqualTo("%backend%");
    }

    @Test
    void percentIsEscaped() {
        assertThat(LikePattern.containing("100%")).isEqualTo("%100\\%%");
    }

    @Test
    void underscoreIsEscaped() {
        assertThat(LikePattern.containing("a_b")).isEqualTo("%a\\_b%");
    }

    @Test
    void backslashIsEscapedFirst() {
        assertThat(LikePattern.containing("a\\b")).isEqualTo("%a\\\\b%");
    }

    @Test
    void lowercasingMatchesTheDatabaseRegardlessOfDefaultLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));

            assertThat(LikePattern.containing("INTERN")).isEqualTo("%intern%");
        } finally {
            Locale.setDefault(original);
        }
    }
}
