import os
import re
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("datathon-docs")

DOCS_DIR = "/docs"

DOCS_METADATA = {
    "README.md": "Guida utente: quick start, credenziali, configurazione, API, troubleshooting",
    "project.md": "Analisi tecnica: architettura, stack, entità JPA, sicurezza, deployment",
}


def _safe_path(name: str) -> str:
    docs_real = os.path.realpath(DOCS_DIR)
    candidate = os.path.realpath(os.path.join(DOCS_DIR, name))
    if not (candidate == docs_real or candidate.startswith(docs_real + os.sep)):
        raise ValueError("Access denied: path outside docs directory")
    if not os.path.isfile(candidate):
        raise ValueError(f"File not found: {name}")
    return candidate


@mcp.tool()
def list_docs() -> str:
    """Returns the list of available documentation files with descriptions."""
    entries = []
    for fname in sorted(os.listdir(DOCS_DIR)):
        if fname.endswith(".md"):
            desc = DOCS_METADATA.get(fname, "")
            entries.append(f"- {fname}: {desc}" if desc else f"- {fname}")
    return "\n".join(entries) if entries else "No documents found."


@mcp.tool()
def read_doc(name: str) -> str:
    """Reads and returns the full content of a documentation file by name."""
    try:
        path = _safe_path(name)
    except ValueError as e:
        return f"ERROR: {e}"
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


@mcp.tool()
def search_docs(query: str, context_lines: int = 3) -> str:
    """Searches all documentation files for a keyword and returns matching sections with context."""
    if not query.strip():
        return "ERROR: query cannot be empty"

    pattern = re.compile(re.escape(query), re.IGNORECASE)
    results = []

    for fname in sorted(os.listdir(DOCS_DIR)):
        if not fname.endswith(".md"):
            continue
        filepath = os.path.join(DOCS_DIR, fname)
        if not os.path.isfile(filepath):
            continue
        with open(filepath, "r", encoding="utf-8") as f:
            lines = f.readlines()

        for i, line in enumerate(lines):
            if pattern.search(line):
                start = max(0, i - context_lines)
                end = min(len(lines), i + context_lines + 1)
                snippet = "".join(lines[start:end])
                results.append(f"[{fname}:L{i+1}]\n{snippet}")

    if not results:
        return f"No results found for: {query}"
    return "\n---\n".join(results)


if __name__ == "__main__":
    mcp.run(transport="stdio")
