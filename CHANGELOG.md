# Changelog

All notable changes to Project 001. Versions follow [Semantic Versioning](https://semver.org); see [docs/RELEASING.md](docs/RELEASING.md) for what each kind of bump means.

## [Unreleased]

## [0.2.1] - 2026-11-21

### Fixed
- Pages now update immediately after a release. Each deploy clears the website's CDN cache, which had been serving the previous version.

### Added
- Version numbers and release notes for every production release.

## [0.2.0] - 2026-11-21

### Added
- Three pages: the home page for everyone (with a "For you / For your parents" demo), **For you** for introverts and busy people, and **For families** for scam protection.
- An admin dashboard at `/admin`: sign-ups, invite counts, CSV export and deletion on request, behind a password.
- An email to the owner for every new waitlist sign-up.
- Search and AI discovery: `robots.txt`, `sitemap.xml`, `llms.txt`, `llms-full.txt`, share images for each page, a web manifest and structured data (including the FAQ).

## [0.1.0] - 2026-11-21

### Added
- The first waitlist page: the family scam-protection story, an animated call demo, privacy rules and an FAQ.
- A working waitlist with queue position and a WhatsApp invite link.
- Open-source licensing: AGPL-3.0 plus a commercial licence, with a contributor agreement.
- Separate dev and prod sites with automatic deploys.

[Unreleased]: https://github.com/LSG-hub/Project-001/compare/v0.2.1...dev
[0.2.1]: https://github.com/LSG-hub/Project-001/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/LSG-hub/Project-001/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/LSG-hub/Project-001/releases/tag/v0.1.0
