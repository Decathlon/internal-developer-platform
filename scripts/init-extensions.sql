-- Initialize PostgreSQL extensions required by the application.
-- This script runs once on first container startup (docker-entrypoint-initdb.d).
CREATE EXTENSION IF NOT EXISTS pg_trgm;
