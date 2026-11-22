# 001 API. Build from services/api:  docker build -t api .
FROM python:3.13-slim AS build
COPY --from=ghcr.io/astral-sh/uv:0.11.19 /uv /bin/uv
ENV UV_COMPILE_BYTECODE=1 UV_LINK_MODE=copy UV_PYTHON_DOWNLOADS=never
WORKDIR /app

# Dependencies first, so code changes don't reinstall them.
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project
COPY src ./src
RUN uv sync --frozen --no-dev --no-editable

FROM python:3.13-slim
RUN useradd --create-home --uid 1000 app
COPY --from=build --chown=app:app /app/.venv /app/.venv
ENV PATH="/app/.venv/bin:$PATH" PYTHONUNBUFFERED=1
USER app
WORKDIR /app
EXPOSE 8080
# Cloud Run sets PORT. One worker per vCPU; async handles many calls per worker.
CMD ["sh", "-c", "exec uvicorn project001_api.main:app --host 0.0.0.0 --port ${PORT:-8080} --proxy-headers --forwarded-allow-ips='*' --timeout-graceful-shutdown 8"]
