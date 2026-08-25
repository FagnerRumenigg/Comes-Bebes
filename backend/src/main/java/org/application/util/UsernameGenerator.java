package org.application.util;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Regra de geração e normalização do @usuário (impl10.md v10 §19.4): minúsculas, sem
 * acento, espaço vira underscore, caractere fora de [a-z0-9_] some, corta em 20,
 * completa até 3 com zeros. A resolução de colisão (que precisa do banco) fica na
 * camada de serviço — esta classe só faz a parte pura.
 */
@Component
public class UsernameGenerator {
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern INVALID_CHARS = Pattern.compile("[^a-z0-9_]");
    private static final int MAX_LENGTH = 20;
    private static final int MIN_LENGTH = 3;

    /**
     * Nunca sugerido nem aceito como @usuário — nomes de sistema, não de pessoa.
     */
    public static final Set<String> RESERVED = Set.of(
            "admin", "administrador", "administrator", "suporte", "support",
            "comesebebes", "comesbebes", "ajuda", "help", "sistema", "system",
            "moderador", "moderator", "root", "api", "www", "null", "undefined",
            "teste", "test");

    public String slugify(String rawName) {
        if (rawName == null) return "";
        String lowercase = rawName.trim().toLowerCase(Locale.ROOT);
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(lowercase, Normalizer.Form.NFD)).replaceAll("");
        String withUnderscores = WHITESPACE.matcher(withoutDiacritics).replaceAll("_");
        String stripped = INVALID_CHARS.matcher(withUnderscores).replaceAll("");
        String truncated = stripped.length() > MAX_LENGTH ? stripped.substring(0, MAX_LENGTH) : stripped;
        return padToMinLength(truncated);
    }

    public boolean isReserved(String candidate) {
        return RESERVED.contains(candidate);
    }

    private String padToMinLength(String value) {
        StringBuilder padded = new StringBuilder(value);
        while (padded.length() < MIN_LENGTH) {
            padded.append('0');
        }
        return padded.toString();
    }
}
