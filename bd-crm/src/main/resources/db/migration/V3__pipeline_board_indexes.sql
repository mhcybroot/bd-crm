CREATE INDEX IF NOT EXISTS idx_leads_template_id ON leads(template_id);
CREATE INDEX IF NOT EXISTS idx_leads_template_stage_id ON leads(template_id, current_stage_id);
CREATE INDEX IF NOT EXISTS idx_leads_priority ON leads(priority);
CREATE INDEX IF NOT EXISTS idx_leads_source ON leads(source);
CREATE INDEX IF NOT EXISTS idx_leads_updated_at ON leads(updated_at);
