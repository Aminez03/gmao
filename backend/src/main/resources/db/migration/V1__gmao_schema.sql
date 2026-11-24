CREATE TABLE app_user (
 id uuid PRIMARY KEY, email varchar(254) NOT NULL UNIQUE, name varchar(160) NOT NULL,
 password_hash varchar(255) NOT NULL, role varchar(40) NOT NULL,
 active boolean NOT NULL DEFAULT true, contractor_id uuid,
 version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL DEFAULT now(),
 CHECK (role IN ('ADMIN','RESPONSABLE_TECHNIQUE','TECHNICIEN','SERVICE_ACHATS','DIRECTION','SOUS_TRAITANT'))
);
CREATE TABLE auth_session (id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES app_user(id),refresh_hash varchar(128) NOT NULL UNIQUE,expires_at timestamptz NOT NULL,revoked boolean NOT NULL DEFAULT false);
CREATE INDEX idx_session_user ON auth_session(user_id);
CREATE TABLE reset_token (id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES app_user(id),hash varchar(128) NOT NULL UNIQUE,expires_at timestamptz NOT NULL,used boolean NOT NULL DEFAULT false);
CREATE TABLE command_receipt (id uuid PRIMARY KEY,user_id uuid NOT NULL REFERENCES app_user(id),operation_key uuid NOT NULL,request_hash varchar(64) NOT NULL,response jsonb NOT NULL,created_at timestamptz NOT NULL DEFAULT now(),UNIQUE(user_id,operation_key));

CREATE TABLE location (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 code varchar(255) NOT NULL,
 name varchar(255) NOT NULL,
 type varchar(255) NOT NULL,
 parent_id uuid
);
CREATE INDEX idx_location_created ON location(created_at DESC);
CREATE TABLE asset (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 code varchar(255) NOT NULL,
 name varchar(255) NOT NULL,
 category varchar(255) NOT NULL,
 location_id uuid NOT NULL,
 parent_id uuid,
 brand varchar(255),
 model varchar(255),
 serial_number varchar(255),
 commissioned_on date,
 supplier_id uuid,
 warranty_until date,
 criticality varchar(255) NOT NULL,
 status varchar(255) NOT NULL,
 qr_token uuid
);
CREATE INDEX idx_asset_created ON asset(created_at DESC);
CREATE TABLE team (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 code varchar(255) NOT NULL,
 name varchar(255) NOT NULL
);
CREATE INDEX idx_team_created ON team(created_at DESC);
CREATE TABLE skill (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 code varchar(255) NOT NULL,
 name varchar(255) NOT NULL
);
CREATE INDEX idx_skill_created ON skill(created_at DESC);
CREATE TABLE user_skill (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 user_id uuid NOT NULL,
 skill_id uuid NOT NULL,
 valid_until date NOT NULL
);
CREATE INDEX idx_user_skill_created ON user_skill(created_at DESC);
CREATE TABLE team_member (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 team_id uuid NOT NULL,
 user_id uuid NOT NULL,
 valid_from date NOT NULL,
 valid_until date
);
CREATE INDEX idx_team_member_created ON team_member(created_at DESC);
CREATE TABLE shift (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 team_id uuid NOT NULL,
 name varchar(255) NOT NULL,
 starts_at timestamptz NOT NULL,
 ends_at timestamptz NOT NULL,
 status varchar(255) NOT NULL
);
CREATE INDEX idx_shift_created ON shift(created_at DESC);
CREATE TABLE shift_assignment (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 shift_id uuid NOT NULL,
 user_id uuid NOT NULL,
 replacement_for_id uuid
);
CREATE INDEX idx_shift_assignment_created ON shift_assignment(created_at DESC);
CREATE TABLE absence (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 user_id uuid NOT NULL,
 starts_at timestamptz NOT NULL,
 ends_at timestamptz NOT NULL,
 reason varchar(255) NOT NULL,
 status varchar(255) NOT NULL
);
CREATE INDEX idx_absence_created ON absence(created_at DESC);
CREATE TABLE handover (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 from_shift_id uuid NOT NULL,
 to_shift_id uuid NOT NULL,
 priority varchar(255) NOT NULL,
 comment text NOT NULL,
 work_order_ids jsonb,
 asset_ids jsonb,
 acknowledged_at timestamptz,
 acknowledged_by uuid
);
CREATE INDEX idx_handover_created ON handover(created_at DESC);
CREATE TABLE checklist (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 name varchar(255) NOT NULL,
 asset_id uuid,
 steps jsonb NOT NULL,
 revision integer NOT NULL,
 published boolean
);
CREATE INDEX idx_checklist_created ON checklist(created_at DESC);
CREATE TABLE preventive_plan (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 name varchar(255) NOT NULL,
 asset_id uuid NOT NULL,
 checklist_id uuid NOT NULL,
 team_id uuid,
 frequency varchar(255) NOT NULL,
 interval_count integer NOT NULL,
 mode varchar(255) NOT NULL,
 anchor_local varchar(255) NOT NULL,
 zone_id varchar(255) NOT NULL,
 next_due_at timestamptz NOT NULL,
 active boolean,
 required_skill_id uuid
);
CREATE INDEX idx_preventive_plan_created ON preventive_plan(created_at DESC);
CREATE TABLE preventive_occurrence (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 plan_id uuid NOT NULL,
 due_at timestamptz NOT NULL,
 status varchar(255) NOT NULL
);
CREATE INDEX idx_preventive_occurrence_created ON preventive_occurrence(created_at DESC);
CREATE TABLE work_order (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 number varchar(255) NOT NULL,
 title varchar(255) NOT NULL,
 description text NOT NULL,
 asset_id uuid NOT NULL,
 plan_id uuid,
 occurrence_id uuid,
 checklist_id uuid,
 checklist_snapshot jsonb,
 answers jsonb,
 type varchar(255) NOT NULL,
 status varchar(255) NOT NULL,
 priority varchar(255) NOT NULL,
 team_id uuid,
 technician_id uuid,
 contractor_id uuid,
 required_skill_id uuid,
 due_at timestamptz,
 started_at timestamptz,
 completed_at timestamptz,
 validated_at timestamptz,
 validated_by uuid,
 wait_reason varchar(255),
 wait_note text,
 diagnostic text,
 cause text,
 solution text,
 report text,
 safety_confirmed boolean,
 actual_minutes integer
);
CREATE INDEX idx_work_order_created ON work_order(created_at DESC);
CREATE TABLE failure_report (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 asset_id uuid NOT NULL,
 description text NOT NULL,
 priority varchar(255) NOT NULL,
 status varchar(255) NOT NULL,
 work_order_id uuid
);
CREATE INDEX idx_failure_report_created ON failure_report(created_at DESC);
CREATE TABLE time_entry (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 work_order_id uuid NOT NULL,
 user_id uuid NOT NULL,
 starts_at timestamptz NOT NULL,
 ends_at timestamptz NOT NULL,
 minutes integer NOT NULL
);
CREATE INDEX idx_time_entry_created ON time_entry(created_at DESC);
CREATE TABLE anomaly (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 work_order_id uuid NOT NULL,
 step_key varchar(255),
 description text NOT NULL,
 severity varchar(255) NOT NULL,
 resolved_at timestamptz,
 resolved_by uuid,
 disposition text,
 corrective_report_id uuid
);
CREATE INDEX idx_anomaly_created ON anomaly(created_at DESC);
CREATE TABLE downtime (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 asset_id uuid NOT NULL,
 work_order_id uuid,
 starts_at timestamptz NOT NULL,
 ends_at timestamptz,
 reason text NOT NULL
);
CREATE INDEX idx_downtime_created ON downtime(created_at DESC);
CREATE TABLE supplier (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 code varchar(255) NOT NULL,
 name varchar(255) NOT NULL,
 email varchar(255),
 phone varchar(255),
 address text
);
CREATE INDEX idx_supplier_created ON supplier(created_at DESC);
CREATE TABLE inventory_item (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 code varchar(255) NOT NULL,
 name varchar(255) NOT NULL,
 unit varchar(255) NOT NULL,
 stock numeric(16,3) NOT NULL,
 reserved numeric(16,3) NOT NULL,
 minimum numeric(16,3) NOT NULL,
 unit_cost numeric(16,3) NOT NULL
);
CREATE INDEX idx_inventory_item_created ON inventory_item(created_at DESC);
CREATE TABLE reservation (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 work_order_id uuid NOT NULL,
 item_id uuid NOT NULL,
 quantity numeric(16,3) NOT NULL,
 allocated numeric(16,3) NOT NULL,
 consumed numeric(16,3) NOT NULL,
 status varchar(255) NOT NULL
);
CREATE INDEX idx_reservation_created ON reservation(created_at DESC);
CREATE TABLE stock_movement (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 item_id uuid NOT NULL,
 work_order_id uuid,
 inspection_id uuid,
 type varchar(255) NOT NULL,
 quantity numeric(16,3) NOT NULL,
 unit_cost numeric(16,3) NOT NULL,
 reason text NOT NULL
);
CREATE INDEX idx_stock_movement_created ON stock_movement(created_at DESC);
CREATE TABLE purchase_request (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 number varchar(255) NOT NULL,
 work_order_id uuid,
 item_id uuid NOT NULL,
 quantity numeric(16,3) NOT NULL,
 justification text NOT NULL,
 status varchar(255) NOT NULL,
 approved_by uuid,
 approved_at timestamptz,
 decision_reason text
);
CREATE INDEX idx_purchase_request_created ON purchase_request(created_at DESC);
CREATE TABLE purchase_order (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 request_id uuid NOT NULL,
 supplier_id uuid NOT NULL,
 quantity numeric(16,3) NOT NULL,
 unit_price numeric(16,3) NOT NULL,
 status varchar(255) NOT NULL,
 expected_on date
);
CREATE INDEX idx_purchase_order_created ON purchase_order(created_at DESC);
CREATE TABLE goods_receipt (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 order_id uuid NOT NULL,
 delivery_reference varchar(255) NOT NULL,
 quantity numeric(16,3) NOT NULL,
 accepted numeric(16,3),
 rejected numeric(16,3),
 status varchar(255) NOT NULL,
 quality_note text
);
CREATE INDEX idx_goods_receipt_created ON goods_receipt(created_at DESC);
CREATE TABLE contractor (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 code varchar(255) NOT NULL,
 name varchar(255) NOT NULL,
 email varchar(255),
 phone varchar(255),
 skills text
);
CREATE INDEX idx_contractor_created ON contractor(created_at DESC);
CREATE TABLE contract (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 contractor_id uuid NOT NULL,
 reference varchar(255) NOT NULL,
 starts_on date NOT NULL,
 ends_on date NOT NULL,
 scope text NOT NULL,
 amount numeric(16,3)
);
CREATE INDEX idx_contract_created ON contract(created_at DESC);
CREATE TABLE external_intervention (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 contract_id uuid NOT NULL,
 work_order_id uuid NOT NULL,
 contractor_id uuid NOT NULL,
 planned_at timestamptz,
 visited_at timestamptz,
 report text,
 status varchar(255) NOT NULL,
 cost numeric(16,3),
 approved_by uuid
);
CREATE INDEX idx_external_intervention_created ON external_intervention(created_at DESC);
CREATE TABLE material_request (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 intervention_id uuid NOT NULL,
 designation varchar(255) NOT NULL,
 quantity numeric(16,3) NOT NULL,
 justification text NOT NULL,
 status varchar(255) NOT NULL
);
CREATE INDEX idx_material_request_created ON material_request(created_at DESC);
CREATE TABLE notification (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 recipient_id uuid NOT NULL,
 title varchar(255) NOT NULL,
 resource_type varchar(255),
 resource_id uuid,
 read_at timestamptz
);
CREATE INDEX idx_notification_created ON notification(created_at DESC);
CREATE TABLE audit_event (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 actor_id uuid,
 action varchar(255) NOT NULL,
 resource_type varchar(255) NOT NULL,
 resource_id uuid,
 details text NOT NULL
);
CREATE INDEX idx_audit_event_created ON audit_event(created_at DESC);
CREATE TABLE attachment (
 id uuid PRIMARY KEY, version bigint NOT NULL DEFAULT 0, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL, created_by uuid REFERENCES app_user(id), archived boolean NOT NULL DEFAULT false,
 target_type varchar(255) NOT NULL,
 target_id uuid NOT NULL,
 name varchar(255) NOT NULL,
 object_key varchar(255) NOT NULL,
 content_type varchar(255) NOT NULL,
 size bigint NOT NULL
);
CREATE INDEX idx_attachment_created ON attachment(created_at DESC);
ALTER TABLE location ADD FOREIGN KEY (parent_id) REFERENCES location(id); CREATE INDEX idx_location_parent_id ON location(parent_id);
ALTER TABLE asset ADD FOREIGN KEY (location_id) REFERENCES location(id); CREATE INDEX idx_asset_location_id ON asset(location_id);
ALTER TABLE asset ADD FOREIGN KEY (parent_id) REFERENCES asset(id); CREATE INDEX idx_asset_parent_id ON asset(parent_id);
ALTER TABLE asset ADD FOREIGN KEY (supplier_id) REFERENCES supplier(id); CREATE INDEX idx_asset_supplier_id ON asset(supplier_id);
ALTER TABLE user_skill ADD FOREIGN KEY (user_id) REFERENCES app_user(id); CREATE INDEX idx_user_skill_user_id ON user_skill(user_id);
ALTER TABLE user_skill ADD FOREIGN KEY (skill_id) REFERENCES skill(id); CREATE INDEX idx_user_skill_skill_id ON user_skill(skill_id);
ALTER TABLE team_member ADD FOREIGN KEY (team_id) REFERENCES team(id); CREATE INDEX idx_team_member_team_id ON team_member(team_id);
ALTER TABLE team_member ADD FOREIGN KEY (user_id) REFERENCES app_user(id); CREATE INDEX idx_team_member_user_id ON team_member(user_id);
ALTER TABLE shift ADD FOREIGN KEY (team_id) REFERENCES team(id); CREATE INDEX idx_shift_team_id ON shift(team_id);
ALTER TABLE shift_assignment ADD FOREIGN KEY (shift_id) REFERENCES shift(id); CREATE INDEX idx_shift_assignment_shift_id ON shift_assignment(shift_id);
ALTER TABLE shift_assignment ADD FOREIGN KEY (user_id) REFERENCES app_user(id); CREATE INDEX idx_shift_assignment_user_id ON shift_assignment(user_id);
ALTER TABLE shift_assignment ADD FOREIGN KEY (replacement_for_id) REFERENCES shift_assignment(id); CREATE INDEX idx_shift_assignment_replacement_for_id ON shift_assignment(replacement_for_id);
ALTER TABLE absence ADD FOREIGN KEY (user_id) REFERENCES app_user(id); CREATE INDEX idx_absence_user_id ON absence(user_id);
ALTER TABLE handover ADD FOREIGN KEY (from_shift_id) REFERENCES shift(id); CREATE INDEX idx_handover_from_shift_id ON handover(from_shift_id);
ALTER TABLE handover ADD FOREIGN KEY (to_shift_id) REFERENCES shift(id); CREATE INDEX idx_handover_to_shift_id ON handover(to_shift_id);
ALTER TABLE handover ADD FOREIGN KEY (acknowledged_by) REFERENCES app_user(id); CREATE INDEX idx_handover_acknowledged_by ON handover(acknowledged_by);
ALTER TABLE checklist ADD FOREIGN KEY (asset_id) REFERENCES asset(id); CREATE INDEX idx_checklist_asset_id ON checklist(asset_id);
ALTER TABLE preventive_plan ADD FOREIGN KEY (asset_id) REFERENCES asset(id); CREATE INDEX idx_preventive_plan_asset_id ON preventive_plan(asset_id);
ALTER TABLE preventive_plan ADD FOREIGN KEY (checklist_id) REFERENCES checklist(id); CREATE INDEX idx_preventive_plan_checklist_id ON preventive_plan(checklist_id);
ALTER TABLE preventive_plan ADD FOREIGN KEY (team_id) REFERENCES team(id); CREATE INDEX idx_preventive_plan_team_id ON preventive_plan(team_id);
ALTER TABLE preventive_plan ADD FOREIGN KEY (required_skill_id) REFERENCES skill(id); CREATE INDEX idx_preventive_plan_required_skill_id ON preventive_plan(required_skill_id);
ALTER TABLE preventive_occurrence ADD FOREIGN KEY (plan_id) REFERENCES preventive_plan(id); CREATE INDEX idx_preventive_occurrence_plan_id ON preventive_occurrence(plan_id);
ALTER TABLE work_order ADD FOREIGN KEY (asset_id) REFERENCES asset(id); CREATE INDEX idx_work_order_asset_id ON work_order(asset_id);
ALTER TABLE work_order ADD FOREIGN KEY (plan_id) REFERENCES preventive_plan(id); CREATE INDEX idx_work_order_plan_id ON work_order(plan_id);
ALTER TABLE work_order ADD FOREIGN KEY (occurrence_id) REFERENCES preventive_occurrence(id); CREATE INDEX idx_work_order_occurrence_id ON work_order(occurrence_id);
ALTER TABLE work_order ADD FOREIGN KEY (checklist_id) REFERENCES checklist(id); CREATE INDEX idx_work_order_checklist_id ON work_order(checklist_id);
ALTER TABLE work_order ADD FOREIGN KEY (team_id) REFERENCES team(id); CREATE INDEX idx_work_order_team_id ON work_order(team_id);
ALTER TABLE work_order ADD FOREIGN KEY (technician_id) REFERENCES app_user(id); CREATE INDEX idx_work_order_technician_id ON work_order(technician_id);
ALTER TABLE work_order ADD FOREIGN KEY (contractor_id) REFERENCES contractor(id); CREATE INDEX idx_work_order_contractor_id ON work_order(contractor_id);
ALTER TABLE work_order ADD FOREIGN KEY (required_skill_id) REFERENCES skill(id); CREATE INDEX idx_work_order_required_skill_id ON work_order(required_skill_id);
ALTER TABLE work_order ADD FOREIGN KEY (validated_by) REFERENCES app_user(id); CREATE INDEX idx_work_order_validated_by ON work_order(validated_by);
ALTER TABLE failure_report ADD FOREIGN KEY (asset_id) REFERENCES asset(id); CREATE INDEX idx_failure_report_asset_id ON failure_report(asset_id);
ALTER TABLE failure_report ADD FOREIGN KEY (work_order_id) REFERENCES work_order(id); CREATE INDEX idx_failure_report_work_order_id ON failure_report(work_order_id);
ALTER TABLE time_entry ADD FOREIGN KEY (work_order_id) REFERENCES work_order(id); CREATE INDEX idx_time_entry_work_order_id ON time_entry(work_order_id);
ALTER TABLE time_entry ADD FOREIGN KEY (user_id) REFERENCES app_user(id); CREATE INDEX idx_time_entry_user_id ON time_entry(user_id);
ALTER TABLE anomaly ADD FOREIGN KEY (work_order_id) REFERENCES work_order(id); CREATE INDEX idx_anomaly_work_order_id ON anomaly(work_order_id);
ALTER TABLE anomaly ADD FOREIGN KEY (resolved_by) REFERENCES app_user(id); CREATE INDEX idx_anomaly_resolved_by ON anomaly(resolved_by);
ALTER TABLE anomaly ADD FOREIGN KEY (corrective_report_id) REFERENCES failure_report(id); CREATE INDEX idx_anomaly_corrective_report_id ON anomaly(corrective_report_id);
ALTER TABLE downtime ADD FOREIGN KEY (asset_id) REFERENCES asset(id); CREATE INDEX idx_downtime_asset_id ON downtime(asset_id);
ALTER TABLE downtime ADD FOREIGN KEY (work_order_id) REFERENCES work_order(id); CREATE INDEX idx_downtime_work_order_id ON downtime(work_order_id);
ALTER TABLE reservation ADD FOREIGN KEY (work_order_id) REFERENCES work_order(id); CREATE INDEX idx_reservation_work_order_id ON reservation(work_order_id);
ALTER TABLE reservation ADD FOREIGN KEY (item_id) REFERENCES inventory_item(id); CREATE INDEX idx_reservation_item_id ON reservation(item_id);
ALTER TABLE stock_movement ADD FOREIGN KEY (item_id) REFERENCES inventory_item(id); CREATE INDEX idx_stock_movement_item_id ON stock_movement(item_id);
ALTER TABLE stock_movement ADD FOREIGN KEY (work_order_id) REFERENCES work_order(id); CREATE INDEX idx_stock_movement_work_order_id ON stock_movement(work_order_id);
ALTER TABLE stock_movement ADD FOREIGN KEY (inspection_id) REFERENCES goods_receipt(id); CREATE INDEX idx_stock_movement_inspection_id ON stock_movement(inspection_id);
ALTER TABLE purchase_request ADD FOREIGN KEY (work_order_id) REFERENCES work_order(id); CREATE INDEX idx_purchase_request_work_order_id ON purchase_request(work_order_id);
ALTER TABLE purchase_request ADD FOREIGN KEY (item_id) REFERENCES inventory_item(id); CREATE INDEX idx_purchase_request_item_id ON purchase_request(item_id);
ALTER TABLE purchase_request ADD FOREIGN KEY (approved_by) REFERENCES app_user(id); CREATE INDEX idx_purchase_request_approved_by ON purchase_request(approved_by);
ALTER TABLE purchase_order ADD FOREIGN KEY (request_id) REFERENCES purchase_request(id); CREATE INDEX idx_purchase_order_request_id ON purchase_order(request_id);
ALTER TABLE purchase_order ADD FOREIGN KEY (supplier_id) REFERENCES supplier(id); CREATE INDEX idx_purchase_order_supplier_id ON purchase_order(supplier_id);
ALTER TABLE goods_receipt ADD FOREIGN KEY (order_id) REFERENCES purchase_order(id); CREATE INDEX idx_goods_receipt_order_id ON goods_receipt(order_id);
ALTER TABLE contract ADD FOREIGN KEY (contractor_id) REFERENCES contractor(id); CREATE INDEX idx_contract_contractor_id ON contract(contractor_id);
ALTER TABLE external_intervention ADD FOREIGN KEY (contract_id) REFERENCES contract(id); CREATE INDEX idx_external_intervention_contract_id ON external_intervention(contract_id);
ALTER TABLE external_intervention ADD FOREIGN KEY (work_order_id) REFERENCES work_order(id); CREATE INDEX idx_external_intervention_work_order_id ON external_intervention(work_order_id);
ALTER TABLE external_intervention ADD FOREIGN KEY (contractor_id) REFERENCES contractor(id); CREATE INDEX idx_external_intervention_contractor_id ON external_intervention(contractor_id);
ALTER TABLE external_intervention ADD FOREIGN KEY (approved_by) REFERENCES app_user(id); CREATE INDEX idx_external_intervention_approved_by ON external_intervention(approved_by);
ALTER TABLE material_request ADD FOREIGN KEY (intervention_id) REFERENCES external_intervention(id); CREATE INDEX idx_material_request_intervention_id ON material_request(intervention_id);
ALTER TABLE notification ADD FOREIGN KEY (recipient_id) REFERENCES app_user(id); CREATE INDEX idx_notification_recipient_id ON notification(recipient_id);
ALTER TABLE audit_event ADD FOREIGN KEY (actor_id) REFERENCES app_user(id); CREATE INDEX idx_audit_event_actor_id ON audit_event(actor_id);
ALTER TABLE app_user ADD FOREIGN KEY(contractor_id) REFERENCES contractor(id);
CREATE UNIQUE INDEX uq_location_code ON location(code);
CREATE UNIQUE INDEX uq_asset_code ON asset(code);
CREATE UNIQUE INDEX uq_asset_qr_token ON asset(qr_token);
CREATE UNIQUE INDEX uq_team_code ON team(code);
CREATE UNIQUE INDEX uq_skill_code ON skill(code);
CREATE UNIQUE INDEX uq_supplier_code ON supplier(code);
CREATE UNIQUE INDEX uq_inventory_item_code ON inventory_item(code);
CREATE UNIQUE INDEX uq_contractor_code ON contractor(code);
CREATE UNIQUE INDEX uq_work_order_number ON work_order(number);
CREATE UNIQUE INDEX uq_work_order_occurrence_id ON work_order(occurrence_id);
CREATE UNIQUE INDEX uq_contract_reference ON contract(reference);
CREATE UNIQUE INDEX uq_purchase_request_number ON purchase_request(number);
CREATE UNIQUE INDEX uq_occurrence ON preventive_occurrence(plan_id,due_at);
CREATE UNIQUE INDEX uq_open_plan ON work_order(plan_id) WHERE plan_id IS NOT NULL AND status NOT IN ('VALIDEE','ANNULEE');
CREATE UNIQUE INDEX uq_receipt ON goods_receipt(order_id,delivery_reference);
CREATE UNIQUE INDEX uq_quality_credit ON stock_movement(inspection_id) WHERE inspection_id IS NOT NULL;
CREATE UNIQUE INDEX uq_shift_assignment ON shift_assignment(shift_id,user_id) WHERE archived=false;
ALTER TABLE inventory_item ADD CHECK(stock>=0 AND reserved>=0 AND reserved<=stock AND minimum>=0 AND unit_cost>=0);
ALTER TABLE reservation ADD CHECK(quantity>0 AND consumed>=0 AND allocated>=consumed AND allocated<=quantity);
ALTER TABLE stock_movement ADD CHECK(quantity>0 AND unit_cost>=0);
ALTER TABLE purchase_request ADD CHECK(quantity>0);
ALTER TABLE purchase_order ADD CHECK(quantity>0 AND unit_price>=0);
ALTER TABLE goods_receipt ADD CHECK(quantity>0 AND coalesce(accepted,0)>=0 AND coalesce(rejected,0)>=0 AND coalesce(accepted,0)+coalesce(rejected,0)<=quantity);
ALTER TABLE shift ADD CHECK(starts_at<ends_at);
ALTER TABLE absence ADD CHECK(starts_at<ends_at);
ALTER TABLE time_entry ADD CHECK(starts_at<ends_at AND minutes>0);
ALTER TABLE preventive_plan ADD CHECK(interval_count>0 AND mode IN ('FIXE','GLISSANT'));
ALTER TABLE work_order ADD CHECK(status IN ('NOUVELLE','AFFECTEE','EN_COURS','EN_ATTENTE','TERMINEE','VALIDEE','ANNULEE'));
CREATE INDEX idx_work_queue ON work_order(status,priority,due_at);
CREATE INDEX idx_due_plan ON preventive_plan(active,next_due_at);
CREATE INDEX idx_user_skill ON user_skill(user_id,skill_id,valid_until);
