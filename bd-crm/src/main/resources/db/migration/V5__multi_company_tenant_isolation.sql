CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(180) NOT NULL,
    status VARCHAR(32) NOT NULL,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    locale VARCHAR(32) NOT NULL DEFAULT 'en-US',
    contact_email VARCHAR(120),
    plan_code VARCHAR(64) NOT NULL DEFAULT 'standard',
    data_retention_days INTEGER NOT NULL DEFAULT 365,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO organizations (slug, name, status, timezone, locale, contact_email, plan_code, data_retention_days)
VALUES ('default', 'Default Organization', 'ACTIVE', 'UTC', 'en-US', 'admin@bdcrm.local', 'standard', 365);

ALTER TABLE users ADD COLUMN organization_id BIGINT;
UPDATE users
SET organization_id = (SELECT id FROM organizations WHERE slug = 'default')
WHERE organization_id IS NULL;
ALTER TABLE users ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE lead_followup_templates ADD COLUMN organization_id BIGINT;
UPDATE lead_followup_templates
SET organization_id = (SELECT id FROM organizations WHERE slug = 'default')
WHERE organization_id IS NULL;
ALTER TABLE lead_followup_templates ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE lead_followup_templates ADD CONSTRAINT fk_templates_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE leads ADD COLUMN organization_id BIGINT;
UPDATE leads
SET organization_id = u.organization_id
FROM users u
WHERE leads.assigned_user_id = u.id
  AND leads.organization_id IS NULL;
ALTER TABLE leads ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE leads ADD CONSTRAINT fk_leads_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE lead_followup_template_steps ADD COLUMN organization_id BIGINT;
UPDATE lead_followup_template_steps s
SET organization_id = t.organization_id
FROM lead_followup_templates t
WHERE s.template_id = t.id
  AND s.organization_id IS NULL;
ALTER TABLE lead_followup_template_steps ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE lead_followup_template_steps ADD CONSTRAINT fk_template_steps_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE template_pipeline_stages ADD COLUMN organization_id BIGINT;
UPDATE template_pipeline_stages s
SET organization_id = t.organization_id
FROM lead_followup_templates t
WHERE s.template_id = t.id
  AND s.organization_id IS NULL;
ALTER TABLE template_pipeline_stages ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE template_pipeline_stages ADD CONSTRAINT fk_pipeline_stages_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE lead_followups ADD COLUMN organization_id BIGINT;
UPDATE lead_followups f
SET organization_id = l.organization_id
FROM leads l
WHERE f.lead_id = l.id
  AND f.organization_id IS NULL;
ALTER TABLE lead_followups ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE lead_followups ADD CONSTRAINT fk_followups_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE lead_notes ADD COLUMN organization_id BIGINT;
UPDATE lead_notes n
SET organization_id = l.organization_id
FROM leads l
WHERE n.lead_id = l.id
  AND n.organization_id IS NULL;
ALTER TABLE lead_notes ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE lead_notes ADD CONSTRAINT fk_lead_notes_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE lead_activities ADD COLUMN organization_id BIGINT;
UPDATE lead_activities a
SET organization_id = l.organization_id
FROM leads l
WHERE a.lead_id = l.id
  AND a.organization_id IS NULL;
ALTER TABLE lead_activities ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE lead_activities ADD CONSTRAINT fk_lead_activities_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE escalation_events ADD COLUMN organization_id BIGINT;
UPDATE escalation_events e
SET organization_id = l.organization_id
FROM leads l
WHERE e.lead_id = l.id
  AND e.organization_id IS NULL;
ALTER TABLE escalation_events ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE escalation_events ADD CONSTRAINT fk_escalation_events_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE lead_stage_history ADD COLUMN organization_id BIGINT;
UPDATE lead_stage_history h
SET organization_id = l.organization_id
FROM leads l
WHERE h.lead_id = l.id
  AND h.organization_id IS NULL;
ALTER TABLE lead_stage_history ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE lead_stage_history ADD CONSTRAINT fk_lead_stage_history_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE lead_qualifications ADD COLUMN organization_id BIGINT;
UPDATE lead_qualifications q
SET organization_id = l.organization_id
FROM leads l
WHERE q.lead_id = l.id
  AND q.organization_id IS NULL;
ALTER TABLE lead_qualifications ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE lead_qualifications ADD CONSTRAINT fk_lead_qualifications_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE lead_communications ADD COLUMN organization_id BIGINT;
UPDATE lead_communications c
SET organization_id = l.organization_id
FROM leads l
WHERE c.lead_id = l.id
  AND c.organization_id IS NULL;
ALTER TABLE lead_communications ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE lead_communications ADD CONSTRAINT fk_lead_communications_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE notification_preferences ADD COLUMN organization_id BIGINT;
UPDATE notification_preferences p
SET organization_id = u.organization_id
FROM users u
WHERE p.user_id = u.id
  AND p.organization_id IS NULL;
ALTER TABLE notification_preferences ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE notification_preferences ADD CONSTRAINT fk_notification_preferences_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE notification_events ADD COLUMN organization_id BIGINT;
UPDATE notification_events
SET organization_id = (
    SELECT l.organization_id
    FROM leads l
    WHERE l.id = notification_events.lead_id
)
WHERE lead_id IS NOT NULL
  AND organization_id IS NULL;
UPDATE notification_events
SET organization_id = (
    SELECT u.organization_id
    FROM users u
    WHERE u.id = notification_events.user_id
)
WHERE organization_id IS NULL;
ALTER TABLE notification_events ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE notification_events ADD CONSTRAINT fk_notification_events_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE duplicate_matches ADD COLUMN organization_id BIGINT;
UPDATE duplicate_matches d
SET organization_id = l.organization_id
FROM leads l
WHERE d.lead_id = l.id
  AND d.organization_id IS NULL;
ALTER TABLE duplicate_matches ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE duplicate_matches ADD CONSTRAINT fk_duplicate_matches_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE lead_merge_events ADD COLUMN organization_id BIGINT;
UPDATE lead_merge_events e
SET organization_id = l.organization_id
FROM leads l
WHERE e.target_lead_id = l.id
  AND e.organization_id IS NULL;
ALTER TABLE lead_merge_events ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE lead_merge_events ADD CONSTRAINT fk_lead_merge_events_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE attachments ADD COLUMN organization_id BIGINT;
UPDATE attachments
SET organization_id = (
    SELECT l.organization_id
    FROM leads l
    WHERE l.id = attachments.lead_id
)
WHERE lead_id IS NOT NULL
  AND organization_id IS NULL;
UPDATE attachments
SET organization_id = (
    SELECT n.organization_id
    FROM lead_notes n
    WHERE n.id = attachments.note_id
)
WHERE note_id IS NOT NULL
  AND organization_id IS NULL;
UPDATE attachments
SET organization_id = (
    SELECT f.organization_id
    FROM lead_followups f
    WHERE f.id = attachments.followup_id
)
WHERE followup_id IS NOT NULL
  AND organization_id IS NULL;
UPDATE attachments
SET organization_id = (
    SELECT u.organization_id
    FROM users u
    WHERE u.id = attachments.uploaded_by_user_id
)
WHERE organization_id IS NULL;
ALTER TABLE attachments ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE attachments ADD CONSTRAINT fk_attachments_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE documents ADD COLUMN organization_id BIGINT;
UPDATE documents d
SET organization_id = l.organization_id
FROM leads l
WHERE d.lead_id = l.id
  AND d.organization_id IS NULL;
ALTER TABLE documents ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE documents ADD CONSTRAINT fk_documents_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE saved_views ADD COLUMN organization_id BIGINT;
UPDATE saved_views s
SET organization_id = u.organization_id
FROM users u
WHERE s.owner_user_id = u.id
  AND s.organization_id IS NULL;
ALTER TABLE saved_views ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE saved_views ADD CONSTRAINT fk_saved_views_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE audit_events ADD COLUMN organization_id BIGINT;
UPDATE audit_events a
SET organization_id = u.organization_id
FROM users u
WHERE a.actor_user_id = u.id
  AND a.organization_id IS NULL;
ALTER TABLE audit_events ADD CONSTRAINT fk_audit_events_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

ALTER TABLE import_jobs ADD COLUMN organization_id BIGINT;
UPDATE import_jobs i
SET organization_id = u.organization_id
FROM users u
WHERE i.requested_by_user_id = u.id
  AND i.organization_id IS NULL;
ALTER TABLE import_jobs ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE import_jobs ADD CONSTRAINT fk_import_jobs_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);

UPDATE roles SET name = 'PLATFORM_ADMIN' WHERE name = 'ADMIN';
UPDATE roles SET name = 'ORG_MANAGER' WHERE name = 'MANAGER';
UPDATE roles SET name = 'ORG_REP' WHERE name = 'REP';
INSERT INTO roles (name)
SELECT 'ORG_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ORG_ADMIN');

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'ORG_ADMIN'
WHERE u.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles ur
      WHERE ur.user_id = u.id
        AND ur.role_id = r.id
  );

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_email_key;
ALTER TABLE lead_followup_templates DROP CONSTRAINT IF EXISTS lead_followup_templates_name_key;

CREATE UNIQUE INDEX ux_users_org_username ON users (organization_id, username);
CREATE UNIQUE INDEX ux_users_org_email ON users (organization_id, email);
CREATE UNIQUE INDEX ux_templates_org_name ON lead_followup_templates (organization_id, name);

CREATE INDEX idx_users_organization_id ON users (organization_id);
CREATE INDEX idx_leads_organization_id ON leads (organization_id);
CREATE INDEX idx_followups_organization_status_due_date ON lead_followups (organization_id, status, due_date);
CREATE INDEX idx_duplicate_matches_organization_id ON duplicate_matches (organization_id);
CREATE INDEX idx_notification_events_organization_id ON notification_events (organization_id);
CREATE INDEX idx_saved_views_organization_page_key ON saved_views (organization_id, page_key);
