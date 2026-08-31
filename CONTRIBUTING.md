# Contributing Guidelines

## Branching & Commit Message Conventions
Follow the Conventional Commits specification:
- `feat:` New features (e.g. `feat: add Loki log stream adapter`)
- `fix:` Bug fixes (e.g. `fix: resolve container state transition race condition`)
- `test:` Adding or updating tests
- `docs:` Documentation improvements
- `refactor:` Code refactoring without behavioral change
- `security:` Security hardening and policy updates

## Pull Request Checklist
1. All Maven unit and integration tests pass: `./mvnw test`
2. Frontend builds cleanly without TypeScript or Lint errors: `cd frontend && npm run build`
3. No hardcoded credentials, API keys, or raw bash execution strings.
