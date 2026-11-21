# Versioning and releases

Every production release has a version and release notes.

## Version numbers: `MAJOR.MINOR.PATCH` ([Semantic Versioning](https://semver.org))

| Bump | When | Examples |
|---|---|---|
| **PATCH** `0.2.0 → 0.2.1` | Fixes and small changes. Nothing new for users to learn. | Copy edits, bug fixes, styling, dependency updates, deploy or infra fixes |
| **MINOR** `0.2.1 → 0.3.0` | A new feature or page that adds to what's there. | A new page, the admin dashboard, sign-up emails, a new call scenario |
| **MAJOR** `0.x → 1.0.0`, `1.x → 2.0.0` | A new generation of the product, or a change that breaks how people use it. | The voice assistant launching to the public (1.0.0), moving to a new product name and domain, a rebuilt app |

**While the version is `0.x`, the product isn't publicly usable yet** (waitlist stage). `1.0.0` marks the first public launch of the assistant itself.

## How to release

1. On a branch, bump `version` in the root `package.json` (and `apps/web/package.json`, kept in sync).
2. Move the items under `## [Unreleased]` in `CHANGELOG.md` into a new `## [X.Y.Z] - YYYY-MM-DD` section, written for people, not commits.
3. Merge into `dev` and check the dev site.
4. Open a PR from `dev` into `main`. The `version-bumped` check fails unless the version is higher than prod's and `CHANGELOG.md` has a section for it.
5. Merge. GitHub Actions deploys prod, clears the CDN cache, tags `vX.Y.Z` and publishes a GitHub Release with that changelog section as its notes.

Changes merged into `dev` without a version bump are fine; they just can't reach `main` until a release PR bumps the version.
