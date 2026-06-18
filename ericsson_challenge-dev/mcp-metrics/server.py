import os
import re
import threading
from http.server import HTTPServer, SimpleHTTPRequestHandler
import httpx
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("datathon-metrics")

BASE_URL = os.environ.get("ACTUATOR_BASE_URL", "http://app:8080/actuator")
TIMEOUT = 5.0
METRIC_NAME_PATTERN = re.compile(r"^[a-zA-Z0-9._-]+$")


def _get(path: str) -> httpx.Response:
    return httpx.get(f"{BASE_URL}{path}", timeout=TIMEOUT)


@mcp.tool()
def health() -> str:
    """Returns the health status of the Spring Boot application from Actuator."""
    try:
        r = _get("/health")
        data = r.json()
        lines = [f"Status: {data.get('status', 'UNKNOWN')}"]
        for name, comp in data.get("components", {}).items():
            lines.append(f"  {name}: {comp.get('status', '?')}")
        return "\n".join(lines)
    except httpx.ConnectError:
        return "ERROR: App non raggiungibile"
    except httpx.TimeoutException:
        return "ERROR: Timeout raggiunto (5s)"
    except Exception as e:
        return f"ERROR: {e}"


@mcp.tool()
def list_metrics() -> str:
    """Returns the list of available metric names from Actuator."""
    try:
        r = _get("/metrics")
        data = r.json()
        names = data.get("names", [])
        return "\n".join(sorted(names)) if names else "No metrics available."
    except httpx.ConnectError:
        return "ERROR: App non raggiungibile"
    except httpx.TimeoutException:
        return "ERROR: Timeout raggiunto (5s)"
    except Exception as e:
        return f"ERROR: {e}"


@mcp.tool()
def get_metric(name: str) -> str:
    """Returns the detail of a specific metric by name (e.g. jvm.memory.used)."""
    if not METRIC_NAME_PATTERN.match(name):
        return "ERROR: Nome metrica non valido. Usa solo lettere, numeri, '.', '_', '-'."
    try:
        r = _get(f"/metrics/{name}")
        if r.status_code == 404:
            return f"ERROR: Metrica '{name}' non trovata."
        data = r.json()
        lines = [
            f"Name: {data.get('name')}",
            f"Description: {data.get('description', 'N/A')}",
            f"Base Unit: {data.get('baseUnit', 'N/A')}",
        ]
        for m in data.get("measurements", []):
            lines.append(f"  {m.get('statistic')}: {m.get('value')}")
        return "\n".join(lines)
    except httpx.ConnectError:
        return "ERROR: App non raggiungibile"
    except httpx.TimeoutException:
        return "ERROR: Timeout raggiunto (5s)"
    except Exception as e:
        return f"ERROR: {e}"


@mcp.tool()
def prometheus() -> str:
    """Returns all metrics in Prometheus exposition format."""
    try:
        r = _get("/prometheus")
        return r.text
    except httpx.ConnectError:
        return "ERROR: App non raggiungibile"
    except httpx.TimeoutException:
        return "ERROR: Timeout raggiunto (5s)"
    except Exception as e:
        return f"ERROR: {e}"


def serve_dashboard():
    """Serve the dashboard HTML on port 9090."""
    os.chdir("/app")
    handler = SimpleHTTPRequestHandler
    server = HTTPServer(("0.0.0.0", 9090), handler)
    server.serve_forever()


if __name__ == "__main__":
    # Start dashboard HTTP server in background
    t = threading.Thread(target=serve_dashboard, daemon=True)
    t.start()
    # Start MCP server on stdio
    mcp.run(transport="stdio")
