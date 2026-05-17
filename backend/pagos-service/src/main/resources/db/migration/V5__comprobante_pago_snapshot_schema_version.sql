-- Versión del formato JSON en snapshot_json (evolución futura sin romper lecturas).
ALTER TABLE comprobante_pago
    ADD COLUMN IF NOT EXISTS snapshot_schema_version INTEGER NOT NULL DEFAULT 1;
