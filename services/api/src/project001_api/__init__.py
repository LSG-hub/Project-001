"""001 backend: the API for the app and, soon, the phone line."""

from importlib.metadata import PackageNotFoundError, version

try:
    __version__ = version("project001-api")
except PackageNotFoundError:  # running from a source tree without install
    __version__ = "0.0.0"
