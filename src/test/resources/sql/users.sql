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
    '$dummyhashedpassword',
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
    '$dummyhashedpassword',
    'John',
    'Smith',
    'ACME Inc',
    'RECRUITER'
);