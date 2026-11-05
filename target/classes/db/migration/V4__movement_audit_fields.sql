-- Guide §6: every stock movement should carry the actor and the reason.
-- Nullable so existing history stays valid; new postings always fill them.

ALTER TABLE inventory_movements ADD COLUMN actor VARCHAR(100);
ALTER TABLE inventory_movements ADD COLUMN reason TEXT;
