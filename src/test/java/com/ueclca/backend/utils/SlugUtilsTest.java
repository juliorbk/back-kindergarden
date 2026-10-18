package com.ueclca.backend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SlugUtilsTest {

    @Test
    void slugifyRemovesAccentsAndUppercase() {
        assertEquals("ano-escolar-2026", SlugUtils.slugify("Año Escolar 2026"));
        assertEquals("matricula-2026-2027", SlugUtils.slugify("¡Matrícula 2026-2027!"));
    }

    @Test
    void slugifyReplacesSymbolsWithSingleDash() {
        assertEquals("cafe-leche", SlugUtils.slugify("Café & Leche"));
        assertEquals("hola-mundo", SlugUtils.slugify("  --Hola __ Mundo--  "));
    }

    @Test
    void slugifyReturnsEmptyWhenThereIsNothingToSlugify() {
        assertEquals("", SlugUtils.slugify("¿¡!?"));
        assertEquals("", SlugUtils.slugify(null));
    }

    @Test
    void uniqueSlugAppendsSuffixOnCollision() {
        assertEquals("titulo", SlugUtils.uniqueSlug("Título", Set.of()::contains));
        assertEquals("titulo-2", SlugUtils.uniqueSlug("Título", Set.of("titulo")::contains));
        assertEquals("titulo-3", SlugUtils.uniqueSlug("Título", Set.of("titulo", "titulo-2")::contains));
    }

    @Test
    void uniqueSlugFallsBackWhenTitleHasNoLettersOrDigits() {
        assertEquals("post", SlugUtils.uniqueSlug("!!!", Set.of()::contains));
        assertEquals("post-2", SlugUtils.uniqueSlug("!!!", Set.of("post")::contains));
    }
}
