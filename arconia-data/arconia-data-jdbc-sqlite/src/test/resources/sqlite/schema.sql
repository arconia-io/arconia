CREATE TABLE book (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT,
    available BOOLEAN NOT NULL,
    isbn TEXT,
    published_at TIMESTAMP,
    version INTEGER
);

CREATE TABLE author (
    book_id INTEGER NOT NULL REFERENCES book (id),
    name TEXT NOT NULL
);
