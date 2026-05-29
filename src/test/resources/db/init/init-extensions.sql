-- Initialize PostgreSQL extensions required by the application.
-- This script runs once when the Testcontainers PostgreSQL container starts.
CREATE EXTENSION IF NOT EXISTS pg_trgm;
