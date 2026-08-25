package org.application.model;

/**
 * Três níveis (produto5.md v5 §6.4, impl10.md v10 §17): PUBLIC ("Público", mesmo sem
 * conta), INTERNAL ("Só para quem tem conta" — a palavra "Interno" está banida da
 * interface, mas o nome do valor no banco não precisa mudar), PRIVATE ("Só para mim" —
 * ninguém além do autor, nunca aparece em feed, busca ou perfil de terceiros).
 */
public enum PublicationVisibility {
    PUBLIC,
    INTERNAL,
    PRIVATE
}
