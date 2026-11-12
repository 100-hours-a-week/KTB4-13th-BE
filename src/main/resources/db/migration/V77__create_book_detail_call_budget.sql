CREATE TABLE book_detail_call_budget (
    id TINYINT NOT NULL PRIMARY KEY,
    quota_day DATE NULL,
    calls INT NOT NULL DEFAULT 0,
    next_call_at DATETIME(6) NULL
);

INSERT INTO book_detail_call_budget (id) VALUES (1);
