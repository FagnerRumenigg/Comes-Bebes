CREATE UNIQUE INDEX users_email_lower_uq ON application.users (lower(email));
CREATE UNIQUE INDEX users_username_lower_uq ON application.users (lower(username));
CREATE UNIQUE INDEX users_blocked_email_hmac_uq ON application.users (blocked_email_hmac) WHERE blocked_email_hmac IS NOT NULL;

CREATE UNIQUE INDEX publications_gcs_object_uq ON application.publications (gcs_bucket, gcs_object_name);

CREATE UNIQUE INDEX recipe_ingredients_active_position_uq
    ON application.recipe_ingredients (recipe_id, position)
    WHERE deleted_at IS NULL;

CREATE INDEX publications_public_feed_idx
    ON application.publications (published_at DESC, id DESC)
    WHERE status = 'ACTIVE' AND visibility = 'PUBLIC';

CREATE INDEX publications_authenticated_feed_idx
    ON application.publications (published_at DESC, id DESC)
    WHERE status = 'ACTIVE';

CREATE INDEX publications_author_idx
    ON application.publications (author_id, published_at DESC, id DESC)
    WHERE status = 'ACTIVE';

CREATE INDEX publications_title_lower_idx ON application.publications (lower(title));
CREATE INDEX recipe_ingredients_name_lower_idx ON application.recipe_ingredients (lower(name));

CREATE INDEX publication_origins_source_recipe_idx ON application.publication_origins (source_recipe_id);
CREATE INDEX publication_image_checks_publication_idx ON application.publication_image_checks (publication_id, checked_at DESC);
CREATE INDEX publication_reactions_active_idx ON application.publication_reactions (publication_id, reaction_type_id) WHERE deleted_at IS NULL;
CREATE INDEX saved_publications_active_idx ON application.saved_publications (user_id, created_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX reports_publication_idx ON application.reports (publication_id);
CREATE INDEX reports_reporter_resolution_idx ON application.reports (reporter_id, resolution, created_at DESC);
CREATE INDEX reports_reporter_created_idx ON application.reports (reporter_id, created_at DESC);
CREATE UNIQUE INDEX moderation_cases_pending_publication_uq
    ON application.moderation_cases (publication_id)
    WHERE status = 'PENDING';
CREATE INDEX moderation_cases_queue_idx ON application.moderation_cases (status, opened_at);
CREATE INDEX user_notifications_inbox_idx
    ON application.user_notifications (user_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX reaction_types_code_uq ON application.reaction_types (code);
CREATE UNIQUE INDEX reaction_types_display_order_uq ON application.reaction_types (display_order);
CREATE UNIQUE INDEX report_reasons_code_uq ON application.report_reasons (code);
CREATE UNIQUE INDEX report_reasons_display_order_uq ON application.report_reasons (display_order);

CREATE UNIQUE INDEX user_notifications_case_type_uq
    ON application.user_notifications (user_id, moderation_case_id, type);
