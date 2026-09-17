-- Flyway migration script: insert_global_admin_team
-- Purpose: Bootstraps the team required for dynamic global authorization.

INSERT INTO entity (id, template_identifier, identifier, name)
SELECT gen_random_uuid(), 'team', 'idp-platform-admins', 'IDP Platform Admins'
WHERE EXISTS (SELECT 1 FROM entity_template WHERE identifier = 'team')
  AND NOT EXISTS (
    SELECT 1 FROM entity
    WHERE template_identifier = 'team'
      AND identifier = 'idp-platform-admins'
  );
