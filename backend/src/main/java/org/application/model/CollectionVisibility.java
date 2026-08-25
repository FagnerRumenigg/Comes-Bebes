package org.application.model;

/**
 * Três níveis (produto5.md v5 §6.3): PUBLIC ("Pública", qualquer um, aparece no perfil),
 * SHARED ("Para quem eu escolher", só convidados por link, não aparece no perfil),
 * PRIVATE ("Só para mim", ninguém além do autor).
 */
public enum CollectionVisibility {
    PUBLIC,
    SHARED,
    PRIVATE
}
