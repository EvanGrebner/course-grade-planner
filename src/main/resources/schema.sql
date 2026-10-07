-- Purpose: Creates the three tables the app needs in PostgreSQL.
-- A course has categories, and a category has assignments.
-- It is safe to run more than once because every table uses IF NOT EXISTS.

CREATE TABLE IF NOT EXISTS courses (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(200)  NOT NULL,
    target_grade  NUMERIC(6, 2) NOT NULL DEFAULT 90
);

-- deleting a course also deletes its categories
CREATE TABLE IF NOT EXISTS categories (
    id            BIGSERIAL PRIMARY KEY,
    course_id     BIGINT        NOT NULL REFERENCES courses (id) ON DELETE CASCADE,
    name          VARCHAR(200)  NOT NULL,
    weight        NUMERIC(6, 2) NOT NULL CHECK (weight >= 0 AND weight <= 100),
    drop_lowest   INTEGER       NOT NULL DEFAULT 0 CHECK (drop_lowest >= 0)
);

-- deleting a category also deletes its assignments
-- earned is empty when the assignment has not been graded yet
CREATE TABLE IF NOT EXISTS assignments (
    id            BIGSERIAL PRIMARY KEY,
    category_id   BIGINT         NOT NULL REFERENCES categories (id) ON DELETE CASCADE,
    name          VARCHAR(200)   NOT NULL,
    earned        NUMERIC(10, 2),
    possible      NUMERIC(10, 2) NOT NULL CHECK (possible >= 0),
    extra_credit  BOOLEAN        NOT NULL DEFAULT FALSE
);

-- indexes make it faster to find the categories of a course and the assignments of a category
CREATE INDEX IF NOT EXISTS idx_categories_course ON categories (course_id);
CREATE INDEX IF NOT EXISTS idx_assignments_category ON assignments (category_id);
