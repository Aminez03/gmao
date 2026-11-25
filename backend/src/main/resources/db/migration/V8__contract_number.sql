ALTER TABLE contract ADD COLUMN number varchar(32);

CREATE SEQUENCE contract_number_seq START WITH 1;

WITH numbered_contracts AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS contract_number
    FROM contract
)
UPDATE contract c
SET number = 'CT-' || lpad(n.contract_number::text, 3, '0')
FROM numbered_contracts n
WHERE c.id = n.id;

SELECT setval(
    'contract_number_seq',
    GREATEST((SELECT count(*) FROM contract), 1),
    EXISTS (SELECT 1 FROM contract)
);

ALTER TABLE contract ALTER COLUMN number SET NOT NULL;
ALTER TABLE contract ADD CONSTRAINT uq_contract_number UNIQUE (number);
