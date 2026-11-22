"""The FastAPI app. Run locally with: uv run uvicorn project001_api.main:app --reload"""

from collections.abc import Awaitable, Callable

from fastapi import FastAPI, Request, Response

from project001_api import __version__
from project001_api.container import Container, build_container
from project001_api.core.errors import register_error_handlers
from project001_api.core.logging import configure_logging, set_trace
from project001_api.core.security import make_current_owner, make_current_uid
from project001_api.core.settings import Env, Settings, get_settings
from project001_api.modules.accounts.router import build_router as accounts_router
from project001_api.modules.calls.router import build_router as calls_router
from project001_api.modules.context.router import build_router as context_router
from project001_api.modules.health.router import build_router as health_router
from project001_api.modules.verification.router import build_router as verification_router


def create_app(settings: Settings | None = None, container: Container | None = None) -> FastAPI:
    settings = settings or get_settings()
    configure_logging(settings.log_level, json_logs=settings.env is not Env.LOCAL)
    container = container or build_container(settings)

    app = FastAPI(
        title="001 API",
        version=__version__,
        # Interactive docs only off prod.
        docs_url=None if settings.is_prod else "/docs",
        redoc_url=None,
        openapi_url=None if settings.is_prod else "/openapi.json",
    )
    app.state.container = container
    register_error_handlers(app)

    @app.middleware("http")
    async def trace_context(request: Request, call_next: Callable[[Request], Awaitable[Response]]) -> Response:
        set_trace(settings.gcp_project, request.headers.get("x-cloud-trace-context"))
        return await call_next(request)

    current_uid = make_current_uid(container.tokens.verify)
    app.include_router(health_router(version=__version__, env=settings.env.value))
    app.include_router(verification_router(container.verification))
    app.include_router(accounts_router(container.accounts, current_uid))
    dev_owner = settings.dev_owner_id if settings.dev_tools_enabled else None
    current_owner = make_current_owner(container.tokens.verify, dev_owner=dev_owner)
    app.include_router(context_router(container.context, current_owner))
    app.include_router(
        calls_router(
            container.calls,
            container.samples,
            current_owner,
            container.tokens.verify,
            test_calls=settings.test_calls_enabled,
            dev_tools=settings.dev_tools_enabled,
            dev_owner=dev_owner,
        )
    )
    return app


app = create_app()
