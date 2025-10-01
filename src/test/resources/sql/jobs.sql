CREATE TABLE jobs(
    id uuid DEFAULT gen_random_uuid(),
    date bigint NOT NULL,
    ownerEmail text NOT NULL,
    company text NOT NULL,
    title text NOT NULL,
    description text NOT NULL,
    externalUrl text NOT NULL,
    remote boolean NOT NULL DEFAULT false,
    location text,
    salaryLo integer,
    salaryHi integer,
    currency text,
    country text,
    tags text[],
    image text,
    seniority text,
    other text,
    active boolean NOT NULL DEFAULT false
);

ALTER TABLE jobs
ADD CONSTRAINT pk_jobs PRIMARY KEY (id);

INSERT INTO jobs (
    id,
    date,
    ownerEmail,
    company,
    title,
    description,
    externalUrl,
    remote,
    location,
    salaryLo,
    salaryHi,
    currency,
    country,
    tags,
    image,
    seniority,
    other,
    active
) VALUES (
    '9283f9f6-65eb-4791-a92c-be32dba0a484', -- id
    1735689600000, -- date
    'hr@acme.com', -- ownerEmail
    'ACME Corp', -- company
    'Senior Scala Developer', -- title
    'We are looking for a skilled Scala developer to join our team and work on high-performance distributed systems.', -- description
    'https://acme.com/careers/scala-developer', -- externalUrl
    true, -- remote
    'San Francisco, CA', -- location
    NULL, -- salaryLo
    NULL, -- salaryHi
    NULL, -- currency
    NULL, -- country
    NULL, -- tags
    NULL, -- image
    NULL, -- seniority
    NULL, -- other
    false -- active
)