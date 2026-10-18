ALTER TABLE patients ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_patients_owner_archived ON patients (owner_id, archived);