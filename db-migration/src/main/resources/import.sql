-- -- Insert default roles
-- INSERT INTO roles (id, name, description) VALUES (1, 'ADMIN', 'Administrator with full access');
-- INSERT INTO roles (id, name, description) VALUES (2, 'USER_MANAGER', 'Can manage users');
-- INSERT INTO roles (id, name, description) VALUES (3, 'ROLE_MANAGER', 'Can manage roles');
-- INSERT INTO roles (id, name, description) VALUES (4, 'USER', 'Regular user');

-- -- Insert admin user (password: admin123)
-- INSERT INTO users (id, username, email, password, full_name, enabled, created_at, updated_at)
-- VALUES (1, 'admin', 'admin@example.com', 'JDJhJDEwJDJYOHk0N2VXZFdQNG9sT0FqVzIuVjZJL1pEMFVjL2JtYVZtOHouN3p4dTVmVzBLMUlJcHhV', 'Administrator', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- -- Assign admin role to admin user
-- INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);

-- -- Insert regular user (password: user123)
-- INSERT INTO users (id, username, email, password, full_name, enabled, created_at, updated_at)
-- VALUES (2, 'user', 'user@example.com', 'JDJhJDEwJDFINW1HbEpnNlQ4dEoyVGVJLzJGdzl6L1ZoTlZYNlYwVE5oU0dOLjRzT1FQVUJJN3dQL0xL', 'Regular User', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- -- Assign user role to regular user
-- INSERT INTO user_roles (user_id, role_id) VALUES (2, 4);