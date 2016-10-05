-- Create an index on the metadata value authority column so that it can be searched efficiently.

DROP INDEX IF EXISTS item_last_modified_idx;

CREATE INDEX item_last_modified_idx ON item (last_modified);