ALTER TABLE Message ADD COLUMN ack_data BINARY(32);
ALTER TABLE Message ADD COLUMN ttl      BIGINT NOT NULL DEFAULT 0;
ALTER TABLE Message ADD COLUMN retries  INT NOT NULL DEFAULT 0;
ALTER TABLE Message ADD COLUMN next_try BIGINT;