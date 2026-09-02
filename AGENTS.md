# Agent Guidelines & Self-Improvement Workflow

This repository uses the self-improving agent workflow to continuously capture learnings, fix errors, and maintain project conventions.

## Project Structure

- **Core App**: Java / RuneLite Plugin (Gradle) in `src/main/java/com/osrsmerch`
- **Analytics & Tooling**: Python (`uv`, Python >= 3.14) in `pyproject.toml`
- **Build**: `./gradlew build` / `gradlew.bat run`

## Self-Improvement Workflow

When encountering errors, tool failures, or corrections during tasks:

1. **Errors**: Append to `.learnings/ERRORS.md` using format `[ERR-YYYYMMDD-XXX]`.
2. **Learnings & Corrections**: Append to `.learnings/LEARNINGS.md` using format `[LRN-YYYYMMDD-XXX]`.
3. **Feature Requests**: Append to `.learnings/FEATURE_REQUESTS.md` using format `[FEAT-YYYYMMDD-XXX]`.
4. **Promotion**: Promote durable conventions and rules directly into `AGENTS.md` or `memory.md`.
