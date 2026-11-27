-- Permanent references for screens and documents. UUID primary/foreign keys remain unchanged.
CREATE FUNCTION format_business_number(prefix text, value bigint) RETURNS text
LANGUAGE sql IMMUTABLE STRICT AS $$
    SELECT prefix || '-' || lpad(value::text, greatest(3, length(value::text)), '0')
$$;

CREATE FUNCTION next_business_number(prefix text, counter regclass) RETURNS text
LANGUAGE sql VOLATILE STRICT AS $$
    SELECT format_business_number(prefix, nextval(counter))
$$;

CREATE FUNCTION assign_display_number() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.display_number IS DISTINCT FROM OLD.display_number THEN
            RAISE EXCEPTION 'A business reference cannot be changed';
        END IF;
        RETURN NEW;
    END IF;

    IF TG_ARGV[2] = 'number' THEN
        NEW.display_number := NEW.number;
    ELSE
        NEW.display_number := next_business_number(TG_ARGV[0], TG_ARGV[1]::regclass);
    END IF;
    RETURN NEW;
END;
$$;

DO $$
DECLARE
    entry record;
    counter_name text;
    largest bigint;
    counter_value bigint;
    counter_called boolean;
BEGIN
    FOR entry IN SELECT * FROM (VALUES
        ('location', 'LOC', false),
        ('asset', 'EQ', false),
        ('team', 'EQP', false),
        ('skill', 'CMP', false),
        ('user_skill', 'HBL', false),
        ('team_member', 'MEM', false),
        ('shift', 'PST', false),
        ('shift_assignment', 'AFF', false),
        ('absence', 'ABS', false),
        ('handover', 'PAS', false),
        ('checklist', 'GAM', false),
        ('preventive_plan', 'PM', true),
        ('preventive_occurrence', 'ECH', false),
        ('work_order', 'OT', true),
        ('failure_report', 'PAN', false),
        ('time_entry', 'TPS', false),
        ('anomaly', 'ANO', false),
        ('downtime', 'ARR', false),
        ('supplier', 'FOU', false),
        ('inventory_item', 'PIE', false),
        ('reservation', 'RES', false),
        ('stock_movement', 'MVT', false),
        ('purchase_request', 'DA', true),
        ('purchase_order', 'BC', false),
        ('goods_receipt', 'BR', false),
        ('contractor', 'PRE', false),
        ('contract', 'CT', true),
        ('external_intervention', 'EXT', false),
        ('material_request', 'DM', false),
        ('notification', 'NOT', false),
        ('audit_event', 'AUD', false),
        ('attachment', 'PJ', false),
        ('asset_reading', 'REL', false),
        ('app_user', 'UT', false)
    ) AS definitions(table_name, prefix, has_number)
    LOOP
        counter_name := CASE entry.table_name
            WHEN 'preventive_plan' THEN 'preventive_plan_number_seq'
            WHEN 'contract' THEN 'contract_number_seq'
            ELSE entry.table_name || '_display_number_seq'
        END;
        EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I', counter_name);
        EXECUTE format('ALTER TABLE %I ADD COLUMN display_number varchar(40)', entry.table_name);

        -- Only enrich the reference column; keep append-only journal contents protected afterwards.
        IF entry.table_name = 'audit_event' THEN
            ALTER TABLE audit_event DISABLE TRIGGER protect_audit;
        ELSIF entry.table_name = 'stock_movement' THEN
            ALTER TABLE stock_movement DISABLE TRIGGER protect_stock_movements;
        END IF;

        -- Keep existing sequential references (especially PM and CT), then number remaining rows.
        IF entry.has_number THEN
            EXECUTE format('UPDATE %I SET display_number = number WHERE number ~ %L',
                entry.table_name, '^' || entry.prefix || '-[0-9]{3,}$');
        END IF;
        EXECUTE format(
            'SELECT coalesce(max(split_part(display_number, ''-'', 2)::bigint), 0) FROM %I',
            entry.table_name
        ) INTO largest;
        EXECUTE format(
            'WITH numbered AS (
                SELECT id, row_number() OVER (ORDER BY created_at, id) + %s AS value
                FROM %I WHERE display_number IS NULL
            ) UPDATE %I target SET display_number = format_business_number(%L, numbered.value)
              FROM numbered WHERE target.id = numbered.id',
            largest, entry.table_name, entry.table_name, entry.prefix
        );
        IF entry.has_number THEN
            EXECUTE format('UPDATE %I SET number = display_number WHERE number IS DISTINCT FROM display_number',
                entry.table_name);
        END IF;

        IF entry.table_name = 'audit_event' THEN
            ALTER TABLE audit_event ENABLE TRIGGER protect_audit;
        ELSIF entry.table_name = 'stock_movement' THEN
            ALTER TABLE stock_movement ENABLE TRIGGER protect_stock_movements;
        END IF;

        EXECUTE format(
            'SELECT coalesce(max(split_part(display_number, ''-'', 2)::bigint), 0) FROM %I',
            entry.table_name
        ) INTO largest;
        EXECUTE format('SELECT last_value, is_called FROM %I', counter_name)
            INTO counter_value, counter_called;
        PERFORM setval(counter_name::regclass, greatest(largest, counter_value, 1), counter_called OR largest > 0);

        EXECUTE format('ALTER TABLE %I ALTER COLUMN display_number SET NOT NULL', entry.table_name);
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I UNIQUE (display_number)',
            entry.table_name, 'uq_' || entry.table_name || '_display_number');
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I CHECK (display_number ~ %L)',
            entry.table_name, 'ck_' || entry.table_name || '_display_number',
            '^' || entry.prefix || '-[0-9]{3,}$');
        EXECUTE format(
            'CREATE TRIGGER business_display_number BEFORE INSERT OR UPDATE OF display_number ON %I
             FOR EACH ROW EXECUTE FUNCTION assign_display_number(%L, %L, %L)',
            entry.table_name, entry.prefix, counter_name,
            CASE WHEN entry.has_number THEN 'number' ELSE '' END
        );
    END LOOP;
END;
$$;
