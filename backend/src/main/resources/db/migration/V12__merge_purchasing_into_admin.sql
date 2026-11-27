-- Transfer purchasing accounts to the administrator role and invalidate their sessions.
WITH promoted AS (
    UPDATE app_user
    SET role = 'ADMIN', version = version + 1
    WHERE role = 'SERVICE_ACHATS'
    RETURNING id
)
UPDATE auth_session
SET revoked = true
WHERE user_id IN (SELECT id FROM promoted);

ALTER TABLE app_user DROP CONSTRAINT app_user_role_check;
ALTER TABLE app_user ADD CONSTRAINT app_user_role_check
    CHECK (role IN ('ADMIN', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN', 'SOUS_TRAITANT'));
