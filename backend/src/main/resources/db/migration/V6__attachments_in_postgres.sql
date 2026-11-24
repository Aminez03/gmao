DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM attachment
        WHERE content IS NULL OR octet_length(content) <> size
    ) THEN
        RAISE EXCEPTION 'Attachment content is missing or has an invalid size; refusing to remove legacy storage references.';
    END IF;
END $$;

CREATE TABLE attachment_content (
    attachment_id UUID PRIMARY KEY REFERENCES attachment(id) ON DELETE CASCADE,
    content BYTEA NOT NULL
);

INSERT INTO attachment_content (attachment_id, content)
SELECT id, content FROM attachment;

ALTER TABLE attachment DROP COLUMN content, DROP COLUMN object_key;
