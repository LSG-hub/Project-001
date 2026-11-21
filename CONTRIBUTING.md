# Contributing

Thanks for your interest in Project 001.

1. Open an issue first for anything larger than a small fix, so we can agree on the approach.
2. Fork, branch, and keep pull requests focused.
3. Run the checks before opening a PR:
   ```bash
   npm install
   npm run lint
   npm test -w @project001/web
   npm run build:web
   ```
4. On your first pull request, a bot asks you to sign the [Contributor License Agreement](CLA.md) by posting one comment. It lets the project stay dual-licensed (AGPL-3.0 plus commercial). See [COMMERCIAL.md](COMMERCIAL.md).

**Never commit secrets:** no keys, tokens, `.env` files, service-account JSON or personal data. `.gitignore` blocks the common ones; check your diff anyway.

**Security issues:** please email me@sreenivasgurram.com instead of opening a public issue.
