ALTER TABLE analyses ADD COLUMN IF NOT EXISTS owner_id VARCHAR(120);

UPDATE analyses
SET owner_id = 'demo'
WHERE owner_id IS NULL;

ALTER TABLE analyses ALTER COLUMN owner_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_analyses_owner_created_at ON analyses(owner_id, created_at DESC);
