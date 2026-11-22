"""Firestore client. Dev uses the `development` database, prod uses `(default)`."""

from google.cloud.firestore import AsyncClient


def make_firestore_client(project: str, database: str) -> AsyncClient:
    return AsyncClient(project=project, database=database)
