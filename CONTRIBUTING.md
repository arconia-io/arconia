# Contributing to Arconia

Thank you for your interest in contributing! Whether it's reporting bugs, suggesting features, improving documentation, or submitting code, your input is welcome and valued.

## Ground Rules

* **Be Respectful.** Adhere to the [Code of Conduct](CODE_OF_CONDUCT.md).
* **Discuss First.** For non-trivial changes, [open an issue](https://github.com/arconia-io/arconia/issues/new/choose) before starting work. PRs without a prior discussion may be rejected.
* **Use Discussions for Questions.** The issue tracker is not for support questions. Use [GitHub Discussions](https://github.com/arconia-io/arconia/discussions) instead.
* **Focused PRs.** Each pull request should address a single issue or feature, linked to the corresponding issue (e.g., `Fixes #123`).
* **Security Vulnerabilities.** Report them responsibly via the [Security Policy](SECURITY.md). Do **not** open a public issue.

## Prerequisites

* Java 21+ installed.
* A container runtime compatible with Testcontainers (e.g., [Podman Desktop](https://podman-desktop.io), [Docker Desktop](https://www.docker.com/products/docker-desktop/)).

## Contribution Workflow

1. **Find or create an issue.** Ensure a [GitHub Issue](https://github.com/arconia-io/arconia/issues) exists for the change you want to make.
2. **Fork and clone** the repository:
    ```shell
    git clone https://github.com/<your-username>/arconia.git
    cd arconia
    ```
3. **Create a branch** off `main`:
    ```shell
    git checkout -b my-feature-branch main
    ```
4. **Implement your changes.** Follow the [code style](#code-style) guidelines, add tests, and update documentation in `docs/` if needed.
5. **Build and test locally:**
    ```shell
    ./gradlew build
    ```
6. **Commit** using [Conventional Commits](#commit-messages) format with a [DCO sign-off](#dco-sign-off):
    ```shell
    git commit -s -m "feat(core): Add new feature"
    ```
7. **Keep your branch updated** via rebase (never merge):
    ```shell
    git fetch upstream
    git rebase upstream/main
    ```
8. **Push and open a PR** targeting `main`. Ensure the PR title follows [Conventional Commits](#commit-messages) format and fill out the PR template.
9. **Address review feedback.** Maintainers will review your PR. Push additional signed-off commits as needed.

## Reporting Bugs and Suggesting Features

Use [GitHub Issues](https://github.com/arconia-io/arconia/issues/new/choose) with the appropriate template:

* **Bugs:** Use the **"Bug: Generic"** template.
* **Features:** Use the **"Request: Feature"** template.
* **Dev Services:** Use the **"Request: Dev Service"** template.

Before submitting, search existing issues to avoid duplicates and ensure bugs are reproducible with the latest version.

## Development Guidelines

### Code Style

* The project uses [.editorconfig](/.editorconfig) for formatting. Ensure your editor respects it.
* Use explicit imports (no wildcards).
* Follow existing alphabetical sorting conventions.

### Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) for both commit messages and PR titles.

```
<type>[optional scope]: <description>
```

* **Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `build`, `ci`, `chore`, `revert`, `deps`.
* **Scopes** (optional, relate to project modules): `core`, `dev`, `k8s`, `multitenancy`, `otel`. Omit scope if the change doesn't relate to one of these modules.
* **Style:** Present tense, imperative mood (e.g., "Add feature", not "Added feature").
* **Breaking changes:** Append `!` after the type/scope (e.g., `feat(core)!:`) or add `BREAKING CHANGE:` in the footer.

Example: `fix(dev): Correct handling of container startup timeout`

### DCO Sign-off

All commits must include a [Developer Certificate of Origin](dco.txt) sign-off. Commits without it will block merging.

```shell
# Sign-off a new commit
git commit -s -m "Your commit message"

# Add sign-off to the last commit
git commit --amend -s --no-edit
```
