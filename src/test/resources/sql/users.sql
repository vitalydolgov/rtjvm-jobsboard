CREATE TABLE users(
    email text NOT NULL,
    hashedPassword text NOT NULL,
    firstName text,
    lastName text,
    company text,
    role text NOT NULL
);

ALTER TABLE users
ADD CONSTRAINT pk_users PRIMARY KEY (email);

INSERT INTO users (
    email,
    hashedPassword,
    firstName,
    lastName,
    company,
    role
) VALUES (
    'admin@example.com',
    '$2a$10$MFtXdkP2q/wDZOBexuF8HuFYMiksRTwHnCDlmcVNvBAflhqqpsYR6', -- passw0rd
    'Admin',
    NULL,
    'Example.com',
    'ADMIN'
);

INSERT INTO users (
    email,
    hashedPassword,
    firstName,
    lastName,
    company,
    role
) VALUES (
    'john@acme.com',
    '$2a$10$XkJYIQhimMez7QamoFdvSOvYLtz8fQlEbQp673OvDlOw9395W08mu', -- password
    'John',
    'Smith',
    'ACME Inc',
    'RECRUITER'
);