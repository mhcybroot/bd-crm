CREATE TABLE duplicate_matches_new (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    matched_lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    match_score INTEGER NOT NULL CHECK (match_score BETWEEN 0 AND 100),
    state VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    reviewed_by_user_id BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_duplicate_matches_canonical_order CHECK (lead_id < matched_lead_id),
    UNIQUE (lead_id, matched_lead_id)
);

INSERT INTO duplicate_matches_new (
    lead_id,
    matched_lead_id,
    match_score,
    state,
    reason,
    reviewed_by_user_id,
    reviewed_at,
    created_at,
    updated_at
)
SELECT
    canonical_lead_id,
    canonical_matched_lead_id,
    match_score,
    state,
    reason,
    reviewed_by_user_id,
    reviewed_at,
    created_at,
    updated_at
FROM (
    SELECT
        CASE WHEN lead_id < matched_lead_id THEN lead_id ELSE matched_lead_id END AS canonical_lead_id,
        CASE WHEN lead_id < matched_lead_id THEN matched_lead_id ELSE lead_id END AS canonical_matched_lead_id,
        match_score,
        state,
        reason,
        reviewed_by_user_id,
        reviewed_at,
        created_at,
        updated_at,
        ROW_NUMBER() OVER (
            PARTITION BY
                CASE WHEN lead_id < matched_lead_id THEN lead_id ELSE matched_lead_id END,
                CASE WHEN lead_id < matched_lead_id THEN matched_lead_id ELSE lead_id END
            ORDER BY updated_at DESC, id DESC
        ) AS row_num
    FROM duplicate_matches
) ranked
WHERE row_num = 1;

DROP TABLE duplicate_matches;
ALTER TABLE duplicate_matches_new RENAME TO duplicate_matches;

CREATE INDEX idx_duplicate_matches_lead_id ON duplicate_matches(lead_id);
CREATE INDEX idx_duplicate_matches_matched_lead_id ON duplicate_matches(matched_lead_id);
