-- Schema + pre-existing data. ddl-auto=none, so the table comes from here.
CREATE TABLE IF NOT EXISTS work_log (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    status VARCHAR(20)  NOT NULL,
    detail VARCHAR(500) NOT NULL
);

TRUNCATE TABLE work_log;

-- pre-existing rows we "already know" -- proves SQL seeding works
INSERT INTO work_log (status, detail) VALUES
    ('SUCCESS', 'seed:pre-existing-1'),
    ('SUCCESS', 'seed:pre-existing-2');
