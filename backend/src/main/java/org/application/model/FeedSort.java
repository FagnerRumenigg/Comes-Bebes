package org.application.model;

/**
 * Facet "Ordenar por" do filtro de feed (docs/telas/05-feed.html). Em RECENT
 * o autenticado ainda vê não-vistas primeiro; em OLDEST a ordem é
 * estritamente cronológica, sem esse agrupamento.
 */
public enum FeedSort {
    RECENT,
    OLDEST
}
