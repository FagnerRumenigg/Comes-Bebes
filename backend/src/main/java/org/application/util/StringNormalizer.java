package org.application.util;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class StringNormalizer {

    public String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
