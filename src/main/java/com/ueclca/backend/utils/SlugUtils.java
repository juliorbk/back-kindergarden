package com.ueclca.backend.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

public final class SlugUtils {

    private static final String FALLBACK_SLUG = "post";

    private SlugUtils() {
    }

    public static String slugify(String input) {
        if (input == null) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-");

        return normalized.replaceAll("^-+|-+$", "");
    }

    public static String uniqueSlug(String input, Predicate<String> exists) {
        String base = slugify(input);
        if (base.isEmpty()) {
            base = FALLBACK_SLUG;
        }

        String candidate = base;
        int suffix = 2;
        while (exists.test(candidate)) {
            candidate = base + "-" + suffix;
            suffix++;
        }
        return candidate;
    }
}
