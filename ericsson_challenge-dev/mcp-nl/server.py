import os
import re
import jaydebeapi
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("datathon-nl")

H2_JDBC_URL = os.environ.get("H2_JDBC_URL", "jdbc:h2:file:/data/datathon_user_db;ACCESS_MODE_DATA=r;DB_CLOSE_ON_EXIT=FALSE")
H2_USER = os.environ.get("H2_USER", "admin")
H2_PASSWORD = os.environ.get("H2_PASSWORD", "dev_password")
H2_JAR_PATH = os.environ.get("H2_JAR_PATH", "/app/h2.jar")

FORBIDDEN_PATTERN = re.compile(
    r"\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|GRANT|REVOKE)\b",
    re.IGNORECASE,
)

# Schema delle relazioni note per generare JOIN
RELATIONS = {
    "users_roles": {"users": "user_id = users.id", "roles": "role_id = roles.id"},
    "eggup_user": {"users": "eni_user_id = users.id", "eggup_score": "score_id = eggup_score.id"},
    "eggup_trait": {"eggup_score": "score_id = eggup_score.id"},
}

# Mappatura NL keywords → SQL components
NL_MAP = {
    "utenti": "users", "utente": "users", "users": "users", "user": "users",
    "ruoli": "roles", "ruolo": "roles", "roles": "roles", "role": "roles",
    "email": "email", "nome": "first_name", "cognome": "last_name",
    "username": "username", "telefono": "phone_number",
    "admin": "ROLE_ADMIN", "amministratore": "ROLE_ADMIN",
    "eggup": "eggup_user", "punteggi": "eggup_score", "tratti": "eggup_trait",
}


def _get_connection():
    try:
        return jaydebeapi.connect(
            "org.h2.Driver", H2_JDBC_URL, [H2_USER, H2_PASSWORD], H2_JAR_PATH
        )
    except Exception as e:
        raise ConnectionError(f"Database non raggiungibile: {e}")


def _get_schema_text():
    conn = _get_connection()
    try:
        cur = conn.cursor()
        cur.execute("""
            SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = 'PUBLIC'
            ORDER BY TABLE_NAME, ORDINAL_POSITION
        """)
        rows = cur.fetchall()
        cur.close()
    finally:
        conn.close()

    tables = {}
    for table, column, dtype, nullable in rows:
        tables.setdefault(table, []).append(f"  {column} ({dtype}, nullable={nullable})")

    output = []
    for table, cols in tables.items():
        output.append(f"{table}:")
        output.extend(cols)
        output.append("")
    return "\n".join(output)


def _translate_nl_to_sql(question: str, schema: str) -> str:
    """Traduzione baseline NL → SQL usando pattern matching."""
    q = question.lower().strip()

    # Conta
    if any(w in q for w in ["quanti", "count", "numero di", "totale"]):
        table = "USERS"
        for kw, tbl in NL_MAP.items():
            if kw in q and tbl in ("users", "roles", "eggup_user", "eggup_score", "eggup_trait"):
                table = tbl.upper()
                break
        return f"SELECT COUNT(*) AS total FROM {table}"

    # Admin users
    if "admin" in q or "amministrator" in q:
        return ("SELECT U.ID, U.USERNAME, U.EMAIL, U.FIRST_NAME, U.LAST_NAME "
                "FROM USERS U "
                "JOIN USERS_ROLES UR ON UR.USER_ID = U.ID "
                "JOIN ROLES R ON R.ID = UR.ROLE_ID "
                "WHERE R.NAME = 'ROLE_ADMIN'")

    # Mostra/lista utenti
    if any(w in q for w in ["mostra", "lista", "elenco", "show", "list"]):
        if any(w in q for w in ["ruol", "role"]):
            return ("SELECT U.USERNAME, U.EMAIL, R.NAME AS ROLE "
                    "FROM USERS U "
                    "JOIN USERS_ROLES UR ON UR.USER_ID = U.ID "
                    "JOIN ROLES R ON R.ID = UR.ROLE_ID")
        if any(w in q for w in ["eggup", "punteg", "score", "trait", "tratt"]):
            return ("SELECT EU.USERNAME, ES.TEST_NAME, ES.COVERAGE_INDEX "
                    "FROM EGGUP_USER EU "
                    "JOIN EGGUP_SCORE ES ON ES.ID = EU.SCORE_ID")
        return "SELECT ID, USERNAME, EMAIL, FIRST_NAME, LAST_NAME FROM USERS"

    # Fallback: select all from users
    return "SELECT ID, USERNAME, EMAIL, FIRST_NAME, LAST_NAME FROM USERS"


@mcp.tool()
def get_schema() -> str:
    """Returns the complete H2 database schema (tables, columns, types) for query context."""
    try:
        return _get_schema_text()
    except ConnectionError as e:
        return str(e)
    except Exception as e:
        return f"Errore nel recupero dello schema: {e}"


@mcp.tool()
def nl_query(question: str) -> str:
    """Translates a natural language question into a proposed SQL query (NOT executed). The user must approve before execution."""
    try:
        schema = _get_schema_text()
    except ConnectionError as e:
        return str(e)
    except Exception as e:
        return f"Errore: {e}"

    sql = _translate_nl_to_sql(question, schema)
    return f"Query SQL proposta (NON eseguita):\n\n{sql}\n\nPer eseguirla, usa il tool 'execute_approved_query' con questa query."


@mcp.tool()
def execute_approved_query(sql: str) -> str:
    """Executes a pre-approved read-only SQL query against the H2 database. Only SELECT is allowed."""
    if FORBIDDEN_PATTERN.search(sql):
        return "ERRORE: Solo query SELECT sono permesse. Operazioni di scrittura bloccate."

    try:
        conn = _get_connection()
    except ConnectionError as e:
        return str(e)

    try:
        cur = conn.cursor()
        cur.execute(sql)
        if cur.description is None:
            cur.close()
            return "ERRORE: La query non ha prodotto risultati (potrebbe non essere un SELECT valido)."
        columns = [desc[0] for desc in cur.description]
        rows = cur.fetchall()
        cur.close()
    except Exception as e:
        return f"Errore nell'esecuzione della query: {e}"
    finally:
        conn.close()

    if not rows:
        return "Nessun risultato trovato."

    lines = [" | ".join(columns)]
    lines.append("-" * len(lines[0]))
    for row in rows[:100]:
        lines.append(" | ".join(str(v) if v is not None else "NULL" for v in row))

    if len(rows) > 100:
        lines.append(f"... ({len(rows)} righe totali, mostrate le prime 100)")

    return "\n".join(lines)


if __name__ == "__main__":
    mcp.run(transport="stdio")
