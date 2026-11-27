ALTER TABLE work_order ADD COLUMN IF NOT EXISTS technician_ids jsonb;
UPDATE work_order
SET technician_ids = jsonb_build_array(technician_id)
WHERE technician_ids IS NULL AND technician_id IS NOT NULL;
UPDATE work_order
SET technician_ids = '[]'::jsonb
WHERE technician_ids IS NULL;
ALTER TABLE work_order ALTER COLUMN technician_ids SET DEFAULT '[]'::jsonb;
ALTER TABLE work_order ALTER COLUMN technician_ids SET NOT NULL;
