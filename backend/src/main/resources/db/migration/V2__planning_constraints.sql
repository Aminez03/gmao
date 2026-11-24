CREATE UNIQUE INDEX uq_shift_start ON shift(starts_at) WHERE archived=false;
ALTER TABLE asset ADD CHECK(criticality IN ('C1','C2','C3','C4'));
ALTER TABLE work_order ADD CHECK(priority IN ('P1','P2','P3','P4'));
ALTER TABLE work_order ADD CHECK(type IN ('PREVENTIVE','CORRECTIVE'));
ALTER TABLE work_order ADD CHECK(status<>'VALIDEE' OR (validated_at IS NOT NULL AND validated_by IS NOT NULL AND completed_at IS NOT NULL));
ALTER TABLE preventive_plan ADD CHECK(frequency IN ('DAILY','WEEKLY','MONTHLY','QUARTERLY','SEMESTRAL','ANNUAL'));
