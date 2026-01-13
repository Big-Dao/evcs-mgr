#!/usr/bin/env python3
"""Markdown style checks (non-archive).

Goals (project-specific):
- Require numeric section numbering for headings (H2+): e.g. '## 1. Title', '### 1.1 Title'.
- Forbid decorative emojis; allow only status/emphasis emojis.

Default behavior is incremental (matches CI):
- If file paths are provided as CLI args, only those files are checked.
- Otherwise, check only changed tracked Markdown files (staged + unstaged).

Optional:
- Use --all to scan README.md + docs/** (excluding docs/archive/).

Exit code:
- 0 when all checks pass
- 1 when any violation is found
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path


ARCHIVE_PREFIX = str(Path("docs") / "archive") + os.sep

# Allowed emoji for status/emphasis (keep this list minimal and intentional).
# Note: some emojis include variation selector (\ufe0f) in text. We strip it before checking.
ALLOWED_EMOJIS = {
    "✅",
    "❌",
    "⚠",
    "ℹ",
    "⛔",
    "🚫",
    "⏳",
    "🟢",
    "🟠",
    "🔴",
    "🟡",
    "🔵",
}

# Approximate emoji/symbol ranges (covers most common emoji + dingbats).
# We intentionally keep this broad; allow-list then filters what is permitted.
EMOJI_RE = re.compile(r"[\U0001F000-\U0001FAFF\u2600-\u27BF]")

# Heading numbering: require a numeric prefix for H2+.
# Accept patterns like:
# - '## 1. Title'
# - '### 1.1 Title'
# - '#### 1.1.1 Title'
# Optional trailing dot after the section id is allowed.
HEADING_RE = re.compile(r"^(#{2,6})\s+(?P<title>.+?)\s*$")
SECTION_PREFIX_RE = re.compile(r"^(\d+(?:\.\d+)*)(\.)?\s+.+")


def should_check_numbered_headings(repo_root: Path, file_path: Path) -> bool:
    rel = file_path.resolve().relative_to(repo_root.resolve()).as_posix()
    if rel == "README.md":
        return True
    if rel.startswith("docs/") and not rel.startswith("docs/archive/"):
        return True
    return False


def iter_markdown_files(repo_root: Path) -> list[Path]:
    files: list[Path] = []

    readme = repo_root / "README.md"
    if readme.exists():
        files.append(readme)

    docs_dir = repo_root / "docs"
    if docs_dir.exists():
        for path in docs_dir.rglob("*.md"):
            rel = path.relative_to(repo_root).as_posix()
            if rel.startswith("docs/archive/"):
                continue
            files.append(path)

    return sorted(set(files))


def git_changed_markdown_files(repo_root: Path) -> list[Path]:
    """Return changed tracked markdown files (staged + unstaged), excluding docs/archive."""

    def _git_names(args: list[str]) -> list[str]:
        try:
            r = subprocess.run(
                ["git", *args],
                cwd=str(repo_root),
                check=False,
                capture_output=True,
                text=True,
            )
        except FileNotFoundError:
            return []
        if r.returncode != 0:
            return []
        return [line.strip() for line in r.stdout.splitlines() if line.strip()]

    # Unstaged + staged changes.
    names = set(_git_names(["diff", "--name-only", "--", "*.md"]))
    names |= set(_git_names(["diff", "--name-only", "--cached", "--", "*.md"]))

    files: list[Path] = []
    for rel in sorted(names):
        if rel.startswith("docs/archive/"):
            continue
        p = (repo_root / rel).resolve()
        if not p.exists():
            continue
        if p.suffix.lower() != ".md":
            continue
        files.append(p)

    return files


def is_archive_path(repo_root: Path, file_path: Path) -> bool:
    rel = file_path.resolve().relative_to(repo_root.resolve()).as_posix()
    return rel.startswith("docs/archive/")


def strip_variation_selectors(s: str) -> str:
    return s.replace("\ufe0f", "")


def find_disallowed_emojis(line: str) -> list[str]:
    normalized = strip_variation_selectors(line)
    found = []
    for match in EMOJI_RE.finditer(normalized):
        ch = match.group(0)
        if ch in ALLOWED_EMOJIS:
            continue
        # Exclude a few non-emoji symbols that are common in technical docs but fall into ranges.
        # Keep this list very small and only for false positives.
        if ch in {"→"}:
            continue
        found.append(ch)
    return found


def check_file(repo_root: Path, file_path: Path) -> list[str]:
    problems: list[str] = []

    check_numbered_headings = should_check_numbered_headings(repo_root, file_path)

    try:
        text = file_path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        text = file_path.read_text(encoding="utf-8", errors="replace")

    lines = text.splitlines()
    in_fenced_block = False
    fence_re = re.compile(r"^\s*(```|~~~)")

    for i, line in enumerate(lines, start=1):
        if fence_re.match(line):
            in_fenced_block = not in_fenced_block
            continue

        if in_fenced_block:
            continue

        m = HEADING_RE.match(line)
        if m and check_numbered_headings:
            title = m.group("title")
            # H2+ must start with numeric section id.
            if not SECTION_PREFIX_RE.match(title):
                problems.append(
                    f"{file_path.relative_to(repo_root)}:{i}: heading must start with numeric section id (e.g. '## 1. ...', '### 1.1 ...'): {line.strip()}"
                )

        bad_emojis = find_disallowed_emojis(line)
        if bad_emojis:
            uniq = "".join(sorted(set(bad_emojis)))
            problems.append(
                f"{file_path.relative_to(repo_root)}:{i}: disallowed emoji/symbol(s) found: {uniq}"
            )

    return problems


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--all",
        action="store_true",
        help="Scan README.md + docs/** (excluding docs/archive/) instead of changed files",
    )
    parser.add_argument("files", nargs="*", help="Markdown files to check")
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parents[1]

    if args.files:
        candidates: list[Path] = []
        for raw in args.files:
            p = (repo_root / raw).resolve() if not os.path.isabs(raw) else Path(raw).resolve()
            if not p.exists():
                continue
            if p.suffix.lower() != ".md":
                continue
            if is_archive_path(repo_root, p):
                continue
            candidates.append(p)
        files = sorted(set(candidates))
    elif args.all:
        files = iter_markdown_files(repo_root)
    else:
        files = git_changed_markdown_files(repo_root)
        if not files:
            print("No non-archive markdown files changed; skipping style check.")
            return 0

    all_problems: list[str] = []
    for f in files:
        all_problems.extend(check_file(repo_root, f))

    if all_problems:
        print("Markdown style violations found:\n", file=sys.stderr)
        for p in all_problems:
            print(p, file=sys.stderr)
        print(
            "\nAllowed emojis are limited to status/emphasis only (e.g. ✅/❌/⚠️). "
            "Headings (H2+) must use numeric section numbering.",
            file=sys.stderr,
        )
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
