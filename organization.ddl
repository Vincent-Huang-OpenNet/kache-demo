CREATE TABLE IF NOT EXISTS organization (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(320) NOT NULL
);

INSERT INTO organization (name, email) VALUES ('azure', 'support@azure.com');
INSERT INTO organization (name, email) VALUES ('aws', 'support@aws.com');
INSERT INTO organization (name, email) VALUES ('gcp', 'support@gcp.com');