package org.application.model;

/**
 * Facet "De quem" do filtro de feed (docs/telas/05-feed.html). FOLLOWING e
 * MY_COLLECTIONS exigem sessão — sem viewerId, o serviço volta pra EVERYONE.
 */
public enum FeedScope {
    EVERYONE,
    FOLLOWING,
    MY_COLLECTIONS
}
