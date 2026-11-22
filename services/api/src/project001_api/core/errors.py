"""One error shape for every API response: {"error": {"code": ..., "message": ...}}."""

import logging

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

log = logging.getLogger(__name__)


class AppError(Exception):
    """An expected failure the client can act on. `code` is stable; `message` is for people."""

    status = 400

    def __init__(self, code: str, message: str, *, status: int | None = None) -> None:
        super().__init__(message)
        self.code = code
        self.message = message
        if status is not None:
            self.status = status


class NotFound(AppError):
    status = 404


class Unauthorized(AppError):
    status = 401


class TooManyRequests(AppError):
    status = 429


class Unavailable(AppError):
    status = 503


def _body(code: str, message: str) -> dict[str, dict[str, str]]:
    return {"error": {"code": code, "message": message}}


def register_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppError)
    async def app_error(_: Request, exc: AppError) -> JSONResponse:
        return JSONResponse(_body(exc.code, exc.message), status_code=exc.status)

    @app.exception_handler(RequestValidationError)
    async def invalid_request(_: Request, exc: RequestValidationError) -> JSONResponse:
        first = exc.errors()[0] if exc.errors() else {}
        where = ".".join(str(p) for p in first.get("loc", ()) if p != "body")
        message = f"{where}: {first.get('msg', 'invalid')}" if where else "Invalid request."
        return JSONResponse(_body("invalid_request", message), status_code=422)

    @app.exception_handler(Exception)
    async def unexpected(_: Request, exc: Exception) -> JSONResponse:
        log.exception("Unhandled error")
        return JSONResponse(_body("internal", "Something went wrong."), status_code=500)
