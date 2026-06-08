-- ==============================================================================
-- CONTACT SERVICE SCHEMA
-- ==============================================================================

CREATE TABLE IF NOT EXISTS contacts (
    id UUID PRIMARY KEY NOT NULL,
    requester_id UUID NOT NULL,
    addressee_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_contact_requester_addressee UNIQUE (requester_id, addressee_id)
);

-- Indexes for quick lookups on requester and addressee
CREATE INDEX IF NOT EXISTS idx_contacts_requester_id ON contacts(requester_id);
CREATE INDEX IF NOT EXISTS idx_contacts_addressee_id ON contacts(addressee_id);
CREATE INDEX IF NOT EXISTS idx_contacts_status ON contacts(status);

-- ==============================================================================
-- COMMENTS
-- ==============================================================================
COMMENT ON TABLE contacts IS 'Stores the contact relationship between users';
COMMENT ON COLUMN contacts.id IS 'Primary key of the contact relationship';
COMMENT ON COLUMN contacts.requester_id IS 'UUID of the user who sent the contact request';
COMMENT ON COLUMN contacts.addressee_id IS 'UUID of the user who received the contact request';
COMMENT ON COLUMN contacts.status IS 'Status of the contact request (PENDING, ACCEPTED)';
