-- Create an index on the metadata value authority column so that it can be searched efficiently.

DROP INDEX IF EXISTS metadatavalue_authority_idx;

ALTER TABLE metadatavalue ALTER COLUMN authority SET DATA TYPE VARCHAR;

CREATE INDEX metadatavalue_authority_idx ON metadatavalue (authority);