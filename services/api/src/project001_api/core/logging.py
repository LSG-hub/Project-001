"""Structured JSON logs that Cloud Logging understands.

Each line carries a `severity`, and the Cloud Trace id of the request when there is
one, so all lines from one request group together in the Logs Explorer.
"""

import json
import logging
import sys
from contextvars import ContextVar
from datetime import UTC, datetime
from typing import Any

_trace: ContextVar[str | None] = ContextVar("trace", default=None)

# Attributes every LogRecord has; anything else passed via `extra=` is logged as a field.
_RESERVED = set(vars(logging.makeLogRecord({}))) | {"message", "asctime"}


def set_trace(project: str, header: str | None) -> None:
    """Remember the trace from Cloud Run's X-Cloud-Trace-Context header for this request."""
    if header:
        trace_id = header.split("/", 1)[0]
        _trace.set(f"projects/{project}/traces/{trace_id}")
    else:
        _trace.set(None)


class CloudJsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        entry: dict[str, Any] = {
            "severity": record.levelname,
            "message": record.getMessage(),
            "logger": record.name,
            "time": datetime.fromtimestamp(record.created, UTC).isoformat(),
        }
        if trace := _trace.get():
            entry["logging.googleapis.com/trace"] = trace
        entry.update(_extras(record))
        if record.exc_info:
            entry["stack_trace"] = self.formatException(record.exc_info)
        return json.dumps(entry, default=str, ensure_ascii=False)


def _extras(record: logging.LogRecord) -> dict[str, Any]:
    return {k: v for k, v in vars(record).items() if k not in _RESERVED and not k.startswith("_")}


class ReadableFormatter(logging.Formatter):
    """Local runs: one readable line, with extra fields as key=value."""

    def format(self, record: logging.LogRecord) -> str:
        line = f"{record.levelname} {record.name}: {record.getMessage()}"
        if extras := _extras(record):
            line += "  " + " ".join(f"{k}={v}" for k, v in extras.items())
        if record.exc_info:
            line += "\n" + self.formatException(record.exc_info)
        return line


def configure_logging(level: str, *, json_logs: bool) -> None:
    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(CloudJsonFormatter() if json_logs else ReadableFormatter())
    root = logging.getLogger()
    root.handlers[:] = [handler]
    root.setLevel(level)
    # Uvicorn's access log duplicates Cloud Run's request log.
    logging.getLogger("uvicorn.access").disabled = True
