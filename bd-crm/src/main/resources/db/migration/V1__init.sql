CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(32) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    manager_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE lead_followup_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lead_followup_template_steps (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES lead_followup_templates(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL CHECK (step_number BETWEEN 1 AND 7),
    day_offset INTEGER NOT NULL CHECK (day_offset >= 0),
    channel VARCHAR(32) NOT NULL,
    instructions VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (template_id, step_number)
);

CREATE TABLE leads (
    id BIGSERIAL PRIMARY KEY,
    company_name VARCHAR(180) NOT NULL,
    contact_name VARCHAR(120) NOT NULL,
    email VARCHAR(120),
    phone VARCHAR(40),
    source VARCHAR(120),
    description TEXT,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(32) NOT NULL,
    assigned_user_id BIGINT NOT NULL REFERENCES users(id),
    template_id BIGINT NOT NULL REFERENCES lead_followup_templates(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lead_followups (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    step_number INTEGER NOT NULL CHECK (step_number BETWEEN 1 AND 7),
    due_date DATE NOT NULL,
    assigned_user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    outcome VARCHAR(32),
    instructions VARCHAR(500),
    notes TEXT,
    completed_at TIMESTAMP WITH TIME ZONE,
    escalated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (lead_id, step_number)
);

CREATE TABLE lead_notes (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id),
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lead_activities (
    id BIGSERIAL PRIMARY KEY,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE escalation_events (
    id BIGSERIAL PRIMARY KEY,
    followup_id BIGINT NOT NULL REFERENCES lead_followups(id) ON DELETE CASCADE,
    lead_id BIGINT NOT NULL REFERENCES leads(id) ON DELETE CASCADE,
    escalated_to_user_id BIGINT REFERENCES users(id),
    days_overdue INTEGER NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_leads_status ON leads(status);
CREATE INDEX idx_leads_assigned_user_id ON leads(assigned_user_id);
CREATE INDEX idx_lead_followups_due_date ON lead_followups(due_date);
CREATE INDEX idx_lead_followups_status ON lead_followups(status);
CREATE INDEX idx_lead_followups_assigned_user_id ON lead_followups(assigned_user_id);

INSERT INTO roles (name) VALUES ('ADMIN'), ('MANAGER'), ('REP');

INSERT INTO users (username, password, full_name, email, active)
VALUES
    ('admin', '{noop}password', 'System Admin', 'admin@bdcrm.local', TRUE),
    ('manager', '{noop}password', 'BD Manager', 'manager@bdcrm.local', TRUE),
    ('rep', '{noop}password', 'BD Rep', 'rep@bdcrm.local', TRUE);

UPDATE users
SET manager_id = (SELECT id FROM users WHERE username = 'manager')
WHERE username = 'rep';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON
    (u.username = 'admin' AND r.name = 'ADMIN')
    OR (u.username = 'manager' AND r.name = 'MANAGER')
    OR (u.username = 'rep' AND r.name = 'REP');

INSERT INTO lead_followup_templates (name, description, is_default, active)
VALUES ('Standard 7 Touch', 'Default 7-step outreach cadence for new leads', TRUE, TRUE);

INSERT INTO lead_followup_template_steps (template_id, step_number, day_offset, channel, instructions)
SELECT t.id, s.step_number, s.day_offset, s.channel, s.instructions
FROM lead_followup_templates t
JOIN (
    VALUES
        (1, 0, 'CALL', 'Initial discovery call'),
        (2, 2, 'EMAIL', 'Share company introduction and value proposition'),
        (3, 5, 'WHATSAPP', 'Short reminder and response check'),
        (4, 7, 'LINKEDIN', 'Connect and re-engage'),
        (5, 10, 'CALL', 'Second call attempt'),
        (6, 14, 'EMAIL', 'Case study or offer follow-up'),
        (7, 21, 'MEETING', 'Final meeting or closeout attempt')
) AS s(step_number, day_offset, channel, instructions) ON TRUE
WHERE t.name = 'Standard 7 Touch';
