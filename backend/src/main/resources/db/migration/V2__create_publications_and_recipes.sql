CREATE TABLE application.publications (
    id uuid PRIMARY KEY,
    author_id uuid NOT NULL REFERENCES application.users(id) ON DELETE RESTRICT,
    type varchar(20) NOT NULL,
    visibility varchar(20) NOT NULL,
    title varchar(255),
    description text,
    gcs_bucket varchar(222) NOT NULL,
    gcs_object_name varchar(1024) NOT NULL,
    gcs_generation bigint NOT NULL,
    image_format varchar(20) NOT NULL,
    image_size_bytes bigint NOT NULL,
    image_width integer NOT NULL,
    image_height integer NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'PENDING_VALIDATION',
    published_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT publications_type_ck CHECK (type IN ('DISH', 'RECIPE', 'MY_VERSION')),
    CONSTRAINT publications_visibility_ck CHECK (visibility IN ('PUBLIC', 'INTERNAL')),
    CONSTRAINT publications_format_ck CHECK (image_format = 'webp'),
    CONSTRAINT publications_generation_ck CHECK (gcs_generation > 0),
    CONSTRAINT publications_size_ck CHECK (image_size_bytes > 0),
    CONSTRAINT publications_dimensions_ck CHECK (image_width > 0 AND image_height > 0),
    CONSTRAINT publications_status_ck CHECK (status IN ('PENDING_VALIDATION', 'ACTIVE', 'UNDER_REVIEW', 'HIDDEN', 'REJECTED', 'REMOVED')),
    CONSTRAINT publications_published_at_ck CHECK (
        (status IN ('ACTIVE', 'UNDER_REVIEW', 'HIDDEN', 'REMOVED') AND published_at IS NOT NULL)
        OR
        (status IN ('PENDING_VALIDATION', 'REJECTED') AND published_at IS NULL)
    ),
    CONSTRAINT publications_description_length_ck CHECK (description IS NULL OR char_length(description) <= 2000)
);

CREATE TABLE application.publication_image_checks (
    id uuid PRIMARY KEY,
    publication_id uuid NOT NULL REFERENCES application.publications(id) ON DELETE CASCADE,
    check_type varchar(30) NOT NULL,
    provider varchar(50) NOT NULL,
    attempt_number smallint NOT NULL,
    status varchar(20) NOT NULL,
    confidence numeric(5,4),
    provider_reference varchar(255),
    error_code varchar(100),
    next_retry_at timestamptz,
    requires_manual_review boolean NOT NULL DEFAULT false,
    checked_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT image_checks_type_ck CHECK (check_type IN ('FOOD', 'PERSON', 'AI_GENERATED')),
    CONSTRAINT image_checks_attempt_ck CHECK (attempt_number BETWEEN 1 AND 3),
    CONSTRAINT image_checks_status_ck CHECK (status IN ('APPROVED', 'REJECTED', 'UNCERTAIN', 'ERROR')),
    CONSTRAINT image_checks_confidence_ck CHECK (confidence IS NULL OR confidence BETWEEN 0 AND 1),
    CONSTRAINT image_checks_unique UNIQUE (publication_id, check_type, provider, attempt_number)
);

CREATE TABLE application.recipes (
    publication_id uuid PRIMARY KEY REFERENCES application.publications(id) ON DELETE CASCADE,
    yield_quantity numeric(10,2),
    yield_unit varchar(50),
    instructions text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT recipes_yield_ck CHECK (yield_quantity IS NULL OR yield_quantity > 0),
    CONSTRAINT recipes_yield_unit_ck CHECK (yield_quantity IS NULL OR (yield_unit IS NOT NULL AND char_length(btrim(yield_unit)) > 0)),
    CONSTRAINT recipes_instructions_ck CHECK (char_length(btrim(instructions)) BETWEEN 1 AND 10000)
);

CREATE TABLE application.recipe_ingredients (
    id uuid PRIMARY KEY,
    recipe_id uuid NOT NULL REFERENCES application.recipes(publication_id) ON DELETE CASCADE,
    position smallint NOT NULL,
    name varchar(150) NOT NULL,
    quantity numeric(12,3),
    unit varchar(50),
    note varchar(255),
    deleted_at timestamptz,
    CONSTRAINT recipe_ingredients_position_ck CHECK (position > 0),
    CONSTRAINT recipe_ingredients_quantity_ck CHECK (quantity IS NULL OR quantity > 0),
    CONSTRAINT recipe_ingredients_name_ck CHECK (char_length(btrim(name)) > 0)
);

CREATE TABLE application.publication_origins (
    derived_publication_id uuid PRIMARY KEY REFERENCES application.publications(id) ON DELETE CASCADE,
    source_recipe_id uuid REFERENCES application.recipes(publication_id) ON DELETE SET NULL,
    source_title_snapshot varchar(150) NOT NULL,
    title_suffix varchar(100) NOT NULL,
    change_summary text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT publication_origins_not_self_ck CHECK (source_recipe_id IS NULL OR source_recipe_id <> derived_publication_id),
    CONSTRAINT publication_origins_snapshot_ck CHECK (char_length(btrim(source_title_snapshot)) > 0),
    CONSTRAINT publication_origins_suffix_ck CHECK (char_length(btrim(title_suffix)) > 0),
    CONSTRAINT publication_origins_summary_length_ck CHECK (change_summary IS NULL OR char_length(change_summary) <= 2000)
);
