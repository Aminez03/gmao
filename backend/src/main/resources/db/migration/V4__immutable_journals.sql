CREATE FUNCTION prevent_journal_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  RAISE EXCEPTION 'Journal append-only: updates and deletes are forbidden';
END;
$$;
CREATE TRIGGER protect_audit BEFORE UPDATE OR DELETE ON audit_event FOR EACH ROW EXECUTE FUNCTION prevent_journal_change();
CREATE TRIGGER protect_stock_movements BEFORE UPDATE OR DELETE ON stock_movement FOR EACH ROW EXECUTE FUNCTION prevent_journal_change();
