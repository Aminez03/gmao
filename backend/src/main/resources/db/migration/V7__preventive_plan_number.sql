ALTER TABLE preventive_plan ADD COLUMN number varchar(32);

CREATE SEQUENCE preventive_plan_number_seq START WITH 1;

WITH numbered_plans AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS plan_number
    FROM preventive_plan
)
UPDATE preventive_plan p
SET number = 'PM-' || lpad(n.plan_number::text, 3, '0')
FROM numbered_plans n
WHERE p.id = n.id;

SELECT setval(
    'preventive_plan_number_seq',
    GREATEST((SELECT count(*) FROM preventive_plan), 1),
    EXISTS (SELECT 1 FROM preventive_plan)
);

ALTER TABLE preventive_plan ALTER COLUMN number SET NOT NULL;
ALTER TABLE preventive_plan ADD CONSTRAINT uq_preventive_plan_number UNIQUE (number);
