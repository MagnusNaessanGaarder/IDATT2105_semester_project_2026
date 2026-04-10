-- Give manager user the MANAGER role (V2 only gave them KITCHEN_MANAGER)
INSERT INTO user_organization_role (user_id, org_number, role_id, assigned_at, assigned_by_user_id)
SELECT u.user_id, 937219997, r.role_id, NOW(), u.user_id
FROM app_user u
         CROSS JOIN role r
WHERE u.email = 'manager@everest-sushi.no'
  AND r.role_name = 'MANAGER'
  AND NOT EXISTS (SELECT 1
                  FROM user_organization_role ex
                           JOIN role er ON ex.role_id = er.role_id
                  WHERE ex.user_id = u.user_id
                    AND ex.org_number = 937219997
                    AND er.role_name = 'MANAGER');