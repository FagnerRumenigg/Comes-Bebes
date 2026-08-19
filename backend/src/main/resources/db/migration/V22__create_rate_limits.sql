CREATE TABLE application.rate_limits (
    scope             text NOT NULL,
    key               text NOT NULL,
    window_started_at timestamptz NOT NULL,
    attempts          integer NOT NULL,
    updated_at        timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (scope, key)
);
