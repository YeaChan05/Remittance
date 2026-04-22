CREATE DATABASE IF NOT EXISTS core;
CREATE DATABASE IF NOT EXISTS integration;

CREATE TABLE IF NOT EXISTS core.account
(
    id             BIGINT         NOT NULL PRIMARY KEY,
    member_id      BIGINT         NOT NULL,
    bank_code      VARCHAR(50)    NOT NULL,
    account_number VARCHAR(50)    NOT NULL,
    account_name   VARCHAR(100)   NOT NULL,
    balance        DECIMAL(19, 2) NOT NULL,
    created_at     DATETIME(6)    NULL,
    updated_at     DATETIME(6)    NULL
);

CREATE TABLE IF NOT EXISTS integration.processed_events
(
    id           BIGINT      NOT NULL PRIMARY KEY,
    event_id     BIGINT      NOT NULL,
    processed_at DATETIME(6) NOT NULL,
    created_at   DATETIME(6) NULL,
    updated_at   DATETIME(6) NULL,
    UNIQUE KEY uk_processed_events_event_id (event_id)
);
