import os
import re
import psycopg2
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("datathon-db")

DB_CONFIG = {
    "host": os.environ.get("DB_HOST", "postgres"),
    "port": int(os.environ.get("DB_PORT", "5432")),
    "dbname": os.environ.get("DB_NAME", "datathon_db"),
    "user": os.environ.get("DB_USERNAME", "datathon_user"),
    "password": os.environ.get("DB_PASSWORD", "datathon_pass"),
}

FORBIDDEN_PATTERN = re.compile(
    r"\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|GRANT|REVOKE)\b",
    re.IGNORECASE,
)


def _get_connection():
    return psycopg2.connect(**DB_CONFIG)


@mcp.tool()
def get_schema() -> str:
    """Returns the database schema (tables, columns, types) for the PostgreSQL database."""
    conn = _get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT table_name, column_name, data_type, is_nullable
                FROM information_schema.columns
                WHERE table_schema = 'public'
                ORDER BY table_name, ordinal_position
            """)
            rows = cur.fetchall()
    finally:
        conn.close()

    result = {}
    for table, column, dtype, nullable in rows:
        if table not in result:
            result[table] = []
        result[table].append(f"  {column} ({dtype}, nullable={nullable})")

    output = []
    for table, cols in result.items():
        output.append(f"{table}:")
        output.extend(cols)
        output.append("")
    return "\n".join(output)


@mcp.tool()
def query_database(sql: str) -> str:
    """Executes a read-only SQL SELECT query against the PostgreSQL database and returns results."""
    if FORBIDDEN_PATTERN.search(sql):
        return "ERROR: Only SELECT queries are allowed."

    conn = _get_connection()
    try:
        with conn.cursor() as cur:
            cur.execute(sql)
            columns = [desc[0] for desc in cur.description]
            rows = cur.fetchall()
    finally:
        conn.close()

    if not rows:
        return "No results found."

    lines = [" | ".join(columns)]
    lines.append("-" * len(lines[0]))
    for row in rows[:100]:
        lines.append(" | ".join(str(v) for v in row))

    if len(rows) > 100:
        lines.append(f"... ({len(rows)} total rows, showing first 100)")

    return "\n".join(lines)


@mcp.tool()
def seed_database() -> str:
    """Populates the database with realistic test data (idempotent, uses ON CONFLICT DO NOTHING)."""
    conn = _get_connection()
    try:
        with conn.cursor() as cur:
            # Roles already exist (created by Spring Boot app)

            # Test users (password = bcrypt hash of 'password')
            hashed = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
            cur.execute(f"""
                INSERT INTO users (id, username, email, first_name, last_name, phone_number, password, created_at, updated_at)
                VALUES
                    (100, 'mario.rossi', 'mario.rossi@test.com', 'Mario', 'Rossi', '+39333111222', '{hashed}', NOW(), NOW()),
                    (101, 'laura.bianchi', 'laura.bianchi@test.com', 'Laura', 'Bianchi', '+39333222333', '{hashed}', NOW(), NOW()),
                    (102, 'paolo.verdi', 'paolo.verdi@test.com', 'Paolo', 'Verdi', '+39333333444', '{hashed}', NOW(), NOW())
                ON CONFLICT DO NOTHING
            """)

            # Users-Roles (no unique constraint, use WHERE NOT EXISTS)
            cur.execute("""
                INSERT INTO users_roles (user_id, role_id)
                SELECT v.user_id, v.role_id FROM (VALUES (100, 1), (101, 2), (102, 2)) AS v(user_id, role_id)
                WHERE NOT EXISTS (
                    SELECT 1 FROM users_roles ur WHERE ur.user_id = v.user_id AND ur.role_id = v.role_id
                )
            """)

            # EggUp Scores
            cur.execute("""
                INSERT INTO eggup_score (id, test_name, coverage_index, duration, date, created_at, updated_at)
                VALUES
                    (100, 'Big Five Assessment', 0.85, 1200, NOW(), NOW(), NOW()),
                    (101, 'Big Five Assessment', 0.72, 980, NOW(), NOW(), NOW())
                ON CONFLICT DO NOTHING
            """)

            # EggUp Users
            cur.execute("""
                INSERT INTO eggup_user (id, eggup_user_guid, username, password, assessment_url, authentication_token, score_id, eni_user_id, created_at, updated_at)
                VALUES
                    (100, 1001, 'user_100', 'randpass123', 'https://eggup.com/assess/100', 'token_abc', 100, 100, NOW(), NOW()),
                    (101, 1002, 'user_101', 'randpass456', 'https://eggup.com/assess/101', 'token_def', 101, 101, NOW(), NOW())
                ON CONFLICT DO NOTHING
            """)

            # EggUp Traits
            cur.execute("""
                INSERT INTO eggup_trait (id, trait_id, trait_name, score, macro_name, macro_score, macro_weight, count, score_id, created_at, updated_at)
                VALUES
                    (100, 'T1', 'Openness', 0.8, 'Macro_Open', 0.75, 0.2, 5, 100, NOW(), NOW()),
                    (101, 'T2', 'Conscientiousness', 0.7, 'Macro_Consc', 0.68, 0.25, 5, 100, NOW(), NOW()),
                    (102, 'T3', 'Extraversion', 0.6, 'Macro_Extra', 0.62, 0.2, 5, 101, NOW(), NOW()),
                    (103, 'T4', 'Agreeableness', 0.9, 'Macro_Agree', 0.85, 0.15, 5, 101, NOW(), NOW())
                ON CONFLICT DO NOTHING
            """)

            conn.commit()

            # Count inserted
            counts = {}
            for table in ['users', 'role', 'users_roles', 'eggup_score', 'eggup_user', 'eggup_trait']:
                cur.execute(f"SELECT COUNT(*) FROM {table}")
                counts[table] = cur.fetchone()[0]

    finally:
        conn.close()

    return "Seed completed. Row counts: " + ", ".join(f"{t}={c}" for t, c in counts.items())


@mcp.tool()
def clean_database() -> str:
    """Removes all test data from the database, preserving only admin user and base roles."""
    conn = _get_connection()
    try:
        with conn.cursor() as cur:
            deleted = {}

            cur.execute("DELETE FROM eggup_trait WHERE id >= 100")
            deleted['eggup_trait'] = cur.rowcount

            cur.execute("DELETE FROM eggup_user WHERE id >= 100")
            deleted['eggup_user'] = cur.rowcount

            cur.execute("DELETE FROM eggup_score WHERE id >= 100")
            deleted['eggup_score'] = cur.rowcount

            cur.execute("""
                DELETE FROM users_roles
                WHERE user_id IN (SELECT id FROM users WHERE email != 'admin@elis.org' AND id >= 100)
            """)
            deleted['users_roles'] = cur.rowcount

            cur.execute("DELETE FROM users WHERE email != 'admin@elis.org' AND id >= 100")
            deleted['users'] = cur.rowcount

            conn.commit()
    finally:
        conn.close()

    return "Clean completed. Deleted: " + ", ".join(f"{t}={c}" for t, c in deleted.items())


if __name__ == "__main__":
    mcp.run(transport="stdio")
