ALTER TABLE leads ADD COLUMN current_stage_id BIGINT;
ALTER TABLE leads ADD COLUMN merged_into_lead_id BIGINT REFERENCES leads(id);
ALTER TABLE leads ADD COLUMN duplicate_state VARCHAR(32) NOT NULL DEFAULT 'CLEAR';

CREATE TABLE template_pipeline_stages (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES lead_followup_templates(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    stage_order INTEGER NOT NULL CHECK (stage_order >= 1),
    sla_hours INTEGER NOT NULL DEFAULT 72 CHECK (sla_hours >= 1),
    exit_automation VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (template_id, stage_order)
);

ALTER TABLE leads
    ADD CONSTRAINT fk_leads_current_stage
    FOREIGN KEY (current_stage_id) REFERENCES template_pipeline_stages(id);

CREATE TABLE lead_stage_history (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    stage_id BIGINT NOT NULL REFERENCES template_pipeline_stages(id),
    changed_by_user_id BIGINT NOT NULL REFERENCES users(id),
    change_note VARCHAR(500),
    entered_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exited_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lead_qualifications (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL UNIQUE REFERENCES leads(id) ON DELETE CASCADE,
    budget_range VARCHAR(120),
    authority_level VARCHAR(120),
    need_summary TEXT,
    timeline_target VARCHAR(120),
    fit_score INTEGER NOT NULL DEFAULT 0 CHECK (fit_score BETWEEN 0 AND 100),
    engagement_score INTEGER NOT NULL DEFAULT 0 CHECK (engagement_score BETWEEN 0 AND 100),
    total_score INTEGER NOT NULL DEFAULT 0 CHECK (total_score BETWEEN 0 AND 100),
    qualification_notes TEXT,
    updated_by_user_id BIGINT REFERENCES users(id),
    qualification_updated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lead_communications (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    channel VARCHAR(32) NOT NULL,
    subject VARCHAR(255),
    body TEXT,
    outcome VARCHAR(64),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    action_url VARCHAR(255),
    lead_id BIGINT REFERENCES leads(id) ON DELETE SET NULL,
    followup_id BIGINT REFERENCES lead_followups(id) ON DELETE SET NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    emailed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE duplicate_matches (
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
    UNIQUE (lead_id, matched_lead_id)
);

CREATE TABLE lead_merge_events (
    id BIGSERIAL PRIMARY KEY,
    source_lead_id BIGINT NOT NULL,
    target_lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    merged_by_user_id BIGINT NOT NULL REFERENCES users(id),
    summary TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attachments (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT REFERENCES leads(id) ON DELETE CASCADE,
    note_id BIGINT REFERENCES lead_notes(id) ON DELETE CASCADE,
    followup_id BIGINT REFERENCES lead_followups(id) ON DELETE CASCADE,
    uploaded_by_user_id BIGINT NOT NULL REFERENCES users(id),
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    file_size BIGINT NOT NULL,
    checksum VARCHAR(120) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    attachment_id BIGINT NOT NULL UNIQUE REFERENCES attachments(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE saved_views (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    page_key VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    shared BOOLEAN NOT NULL DEFAULT FALSE,
    config_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id BIGINT,
    description TEXT NOT NULL,
    details_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE import_jobs (
    id BIGSERIAL PRIMARY KEY,
    requested_by_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    summary_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO template_pipeline_stages (template_id, name, stage_order, sla_hours, exit_automation)
SELECT t.id, s.name, s.stage_order, s.sla_hours, s.exit_automation
FROM lead_followup_templates t
JOIN (
    VALUES
        ('Prospecting', 1, 48, 'START_FIRST_TOUCH'),
        ('Contacted', 2, 72, 'FOLLOWUP_REMINDER'),
        ('Qualified', 3, 72, 'RECOMMEND_MEETING'),
        ('Proposal', 4, 96, 'PROPOSAL_TRACKING'),
        ('Negotiation', 5, 120, 'MANAGER_REVIEW')
) AS s(name, stage_order, sla_hours, exit_automation) ON TRUE
WHERE t.name = 'Standard 7 Touch';

UPDATE leads l
SET current_stage_id = s.id
FROM template_pipeline_stages s
WHERE s.template_id = l.template_id
  AND s.stage_order = 1
  AND l.current_stage_id IS NULL;

CREATE INDEX idx_leads_current_stage_id ON leads(current_stage_id);
CREATE INDEX idx_lead_stage_history_lead_id ON lead_stage_history(lead_id);
CREATE INDEX idx_notification_events_user_id ON notification_events(user_id);
CREATE INDEX idx_duplicate_matches_lead_id ON duplicate_matches(lead_id);
CREATE INDEX idx_duplicate_matches_matched_lead_id ON duplicate_matches(matched_lead_id);
CREATE INDEX idx_saved_views_owner_user_id ON saved_views(owner_user_id);
CREATE INDEX idx_audit_events_event_type ON audit_events(event_type);
CREATE INDEX idx_attachments_lead_id ON attachments(lead_id);
CREATE INDEX idx_documents_lead_id ON documents(lead_id);
