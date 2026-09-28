-- Allow PENDING_REVIEW for held high-risk transfers (ATO slice).
ALTER TABLE transfers DROP CONSTRAINT IF EXISTS transfers_status_check;
ALTER TABLE transfers ADD CONSTRAINT transfers_status_check
    CHECK (status IN ('PENDING', 'PENDING_REVIEW', 'COMPLETED', 'FAILED', 'FLAGGED'));
