-- Flyway migration script to create Property table

CREATE TABLE property (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    value VARCHAR(255) NOT NULL
);
