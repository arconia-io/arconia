# Contributing to Arconia

Thank you for considering contributing to Arconia! We appreciate your time and effort in helping make the project better. These guidelines aim to streamline the contribution process, ensure clarity in communication, and align your efforts with the project's goals.

Arconia is an open-source project that welcomes contributions from everyone. Whether it's reporting bugs, suggesting features, improving documentation, or submitting code changes via pull requests, your input is valuable. Please follow these guidelines to ensure a smooth and efficient collaboration.

## Table of Contents

- [Ground Rules](#ground-rules)
- [Getting Started: Prerequisites](#getting-started-prerequisites)
- [Contribution Workflow: Issues and Pull Requests](#contribution-workflow-issues-and-pull-requests)
- [Reporting Bugs and Suggesting Features](#reporting-bugs-and-suggesting-features)
- [Asking Support Questions](#asking-support-questions)
- [Reporting Security Vulnerabilities](#reporting-security-vulnerabilities)
- [Development Guidelines](#development-guidelines)
  - [Code Style](#code-style)
  - [Commit Messages](#commit-messages)
  - [Signing Commits](#signing-commits)
    - [Developer Certificate of Origin (DCO) Sign-off](#developer-certificate-of-origin-dco-sign-off)
    - [GPG/SSH Commit Signing](#gpgssh-commit-signing)
  - [Rebasing and Squashing Commits](#rebasing-and-squashing-commits)
- [Code of Conduct](#code-of-conduct)

## Ground Rules

*   **Be Respectful:** Interact politely and respectfully with everyone in the community. Adhere to the [Arconia Code of Conduct](CODE_OF_CONDUCT.md).
*   **Discuss First:** For any non-trivial changes (more than fixing a typo), **please open a [GitHub Issue](https://github.com/arconia-io/arconia/issues/new/choose) first.** Discuss your proposed changes and get feedback before starting work. This prevents wasted effort and ensures alignment. Failure to do so might result in your pull request being rejected.
*   **Use Discussions for Questions:** For general questions or help, please use [GitHub Discussions](https://github.com/arconia-io/arconia/discussions).
*   **Focused Pull Requests:** Keep each pull request focused on a single issue or feature. Link the PR to the corresponding issue (e.g., `Fixes #123` or `Closes gh-XXXX` in the PR description).

## Getting Started: Prerequisites

Before you start contributing code, ensure you have:

*   Java 21 or later installed.
*   A container runtime compatible with Testcontainers (e.g., [Podman Desktop](https://podman-desktop.io), [Docker Desktop](https://www.docker.com/products/docker-desktop/)).

## Contribution Workflow: Issues and Pull Requests

Contributions are made via GitHub Pull Requests. Here’s the typical workflow:

1.  **Find or Create an Issue:** Ensure a [GitHub Issue](https://github.com/arconia-io/arconia/issues) exists for the bug or feature you want to address. If not, create one to discuss the change first (see [Ground Rules](#ground-rules)).
2.  **Fork and Clone:** Fork the `arconia-io/arconia` repository on GitHub and clone your fork locally:
    ```shell
    git clone https://github.com/<your-username>/arconia.git
    cd arconia
    ```
3.  **Create a Branch:** Create a descriptive branch for your changes off the `main` branch:
    ```shell
    git checkout -b my-descriptive-feature-branch main
    ```
4.  **Implement Changes:** Make your code changes. Follow the [Code Style](#code-style) guidelines.
5.  **Add Tests:** Write unit or integration tests covering your changes.
6.  **Update Documentation:** If necessary, update documentation in the `docs/` directory.
7.  **Build and Test Locally:** Ensure everything builds and all tests pass:
    ```shell
    ./gradlew build
    ```
8.  **Commit Changes:** Commit your work using descriptive messages that follow the [Commit Messages](#commit-messages) format. Crucially, ensure your commits are signed off and signed (see [Signing Commits](#signing-commits)).
    ```shell
    git add .
    git commit -S -s -m "feat(core): Implement the new feature"
    ```
9.  **Keep Branch Updated:** Before pushing, and periodically during development, update your branch with the latest changes from the upstream `main` branch using rebase (NEVER merge):
    ```shell
    # Add upstream remote if you haven't already
    git remote add upstream https://github.com/arconia-io/arconia.git

    # Fetch latest changes and rebase your branch
    git fetch upstream
    git rebase upstream/main
    # Resolve any conflicts if they occur
    ```
10. **Push to Your Fork:** Push your branch to your fork. Use `--force-with-lease` if you rebased or amended commits:
    ```shell
    git push origin my-descriptive-feature-branch --force-with-lease
    ```
11. **Open a Pull Request:**
    *   Navigate to the `arconia-io/arconia` repository on GitHub.
    *   Click "New pull request" and choose to compare across forks, selecting your fork and branch.
    *   Target the `main` branch of `arconia-io/arconia`.
    *   Ensure the PR title follows the [Commit Messages](#commit-messages) format.
    *   Fill out the pull request template, clearly describing the changes and linking the related issue (e.g., `Fixes #123`).
12. **Code Review:** Project maintainers will review your PR. Address any feedback by making changes on your branch, committing them (signed and signed-off), and pushing again. The PR will update automatically.
13. **Rebase and Squash (If Necessary):** Before merging, ensure your branch is rebased on the latest `upstream/main`. If your PR contains multiple commits (e.g., from addressing review feedback), squash them into a single logical commit. See [Rebasing and Squashing Commits](#rebasing-and-squashing-commits) for details.
14. **Merge:** Once the PR is approved and all checks pass, a maintainer will merge it. Congratulations and thank you for your contribution!

## Reporting Bugs and Suggesting Features

Use our [GitHub Issues](https://github.com/arconia-io/arconia/issues/new/choose) page to report bugs or suggest features, selecting the appropriate template:

*   **Bugs:** Use the **"Bug: Generic"** template.
*   **Features:** Use the **"Request: Feature"** template.
*   **Dev Services:** Use the **"Request: Dev Service"** template.

Before submitting:

1.  Search existing issues to avoid duplicates.
2.  For bugs, ensure it's reproducible with the latest version.
3.  Provide clear descriptions and follow the template instructions.
4.  Remember to discuss significant changes *before* starting implementation (see [Ground Rules](#ground-rules)).

## Asking Support Questions

Please **do not** use the issue tracker for support questions. Use [GitHub Discussions](https://github.com/arconia-io/arconia/discussions) instead.

## Reporting Security Vulnerabilities

If you discover a security vulnerability, report it responsibly by following the instructions in our [Security Policy](SECURITY.md). **Do NOT open a public issue or disclose it publicly.**

## Development Guidelines

### Code Style

*   The project uses [.editorconfig](/.editorconfig) to define basic code formatting. Please ensure your editor respects this file.
*   Use explicit imports; avoid wildcard (`*`) imports.
*   Follow the existing sorting order when adding items to lists (usually alphabetical).

### Commit Messages

We follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification for PR titles and commit messages. This aids automated releases and improves history readability.

Format:
```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

*   **Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `build`, `ci`, `chore`, `revert`, `deps`.
*   **Scopes** (optional, relate to project modules): `core`, `dev`, `k8s`, `multitenancy`, `otel`. Do not include a scope if the change doesn't relate to one of these modules.
*   **Description**: Use present tense ("Add feature", not "Added feature") and imperative mood ("Move cursor", not "Moves cursor").
*   **Breaking Changes**: Indicate breaking changes with `BREAKING CHANGE:` in the footer or by appending `!` after the type/scope (e.g., `feat(core)!:`).

Example: `fix(dev): Correct handling of container startup timeout`

The Pull Request title *must* also follow this convention.

### Signing Commits

All commits contributed to Arconia **must be**:

1.  **Signed-off:** Attesting to the [Developer Certificate of Origin (DCO)](#developer-certificate-of-origin-dco-sign-off).
2.  **Signed:** Verifying the author's identity using [GPG or SSH keys](#gpgssh-commit-signing).

Commits lacking either sign-off or signature verification will block merging.

#### Developer Certificate of Origin (DCO) Sign-off

The DCO certifies you have the right to submit your contribution. Add a `Signed-off-by:` line to your commit messages:

```
Signed-off-by: Your Name <your.email@example.com>
```

The easiest way is using the `-s` flag during commit:
```shell
git commit -s -m "Your commit message"
```

To add a sign-off to your *last* commit if you forgot:
```shell
git commit --amend -s --no-edit
```
For older commits, use interactive rebase (`git rebase -i`).

#### GPG/SSH Commit Signing

Cryptographic signing verifies commit author identity. Configure Git to sign commits automatically using a GPG or SSH key linked to your GitHub account.

**Configuration:**
```shell
# Option 1: Using GPG
git config --global commit.gpgsign true
git config --global user.signingkey YOUR_GPG_KEY_ID # Find with: gpg --list-secret-keys --keyid-format=long

# Option 2: Using SSH (requires Git 2.34+)
git config --global gpg.format ssh
git config --global user.signingkey PATH_TO_YOUR_SSH_PUBLIC_KEY # e.g., ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
```

Refer to GitHub's documentation for details:
*   [Managing commit signature verification](https://docs.github.com/en/authentication/managing-commit-signature-verification)
*   [Signing commits](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits)

**Committing:**
Once configured, Git should sign automatically. You can also explicitly use the `-S` flag (combine with `-s` for DCO):
```shell
git commit -S -s -m "Your commit message"
```

> [!TIP]
> To add *both* sign-off (`-s`) and signature (`-S`) to the *last* commit if forgotten:
> ```shell
> git commit --amend -S -s --no-edit
> ```

### Rebasing and Squashing Commits

Keep your feature branch updated by rebasing onto the upstream `main` branch. **Never merge `main` into your feature branch.**

```shell
# Fetch latest upstream changes
git fetch upstream

# Rebase your branch
git rebase upstream/main
# Resolve any conflicts that arise
```

If your pull request includes multiple commits (e.g., "Fix typo", "Address review comments"), squash them into a single, logical commit before merging. This keeps the project history clean.

**How to Squash (Example: Squashing last 3 commits):**

1.  Ensure your branch is rebased onto `upstream/main`.
2.  Run interactive rebase: `git rebase -i HEAD~3`.
3.  Your editor will open. Keep the first commit as `pick`, change the others to `squash` (or `s`):
    ```
    pick a1b2c3d feat(core): Implement the main feature
    squash e4f5g6h fix: Address review comment
    squash i7j8k9l style: Minor code cleanup
    ```
4.  Save and close. A new editor window opens for the combined commit message.
5.  Edit the message to be clear and concise, following the [Conventional Commits](#commit-messages) format. **Crucially, ensure the `Signed-off-by:` line is present and that the commit will be GPG/SSH signed** (this usually happens automatically if configured, or use `git commit --amend -S -s` after the rebase if needed).
6.  Save and close the commit message editor.
7.  Force-push the squashed commit to your fork:
    ```shell
    git push origin your-branch-name --force-with-lease
    ```

Maintainers *can* squash commits via the GitHub UI during merge, but doing it yourself beforehand is preferred.

## Code of Conduct

All participants in the Arconia community are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please read it to understand the expected standards of behavior. Treat everyone with respect and kindness.
