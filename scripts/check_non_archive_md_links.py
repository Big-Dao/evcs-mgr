#!/usr/bin/env python3

import re
import subprocess
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]

LINK_RE = re.compile(r"\[[^\]]*\]\(([^)]+)\)")


def iter_links(text: str):
	in_fence = False
	for raw_line in text.splitlines():
		line = raw_line.rstrip("\n")
		if line.strip().startswith("```"):
			in_fence = not in_fence
			continue
		if in_fence:
			continue
		for match in LINK_RE.finditer(line):
			yield match.group(1).strip()


def is_external(link: str) -> bool:
	return bool(re.match(r"^[a-zA-Z][a-zA-Z0-9+.-]*:", link))


def normalize_link_path(link: str) -> str:
	if link.startswith("<") and link.endswith(">"):
		link = link[1:-1]
	link = link.strip()
	if not link or link.startswith("#"):
		return ""
	if is_external(link):
		return ""
	return link.split("#", 1)[0].split("?", 1)[0]


def resolve_target(md_file: Path, link_path: str) -> Path:
	if link_path.startswith("/"):
		return (REPO / link_path.lstrip("/")).resolve()
	return (md_file.parent / link_path).resolve()


def main() -> int:
	tracked = subprocess.check_output(["git", "ls-files", "*.md"], cwd=REPO, text=True)
	md_files = [REPO / p for p in tracked.splitlines() if p and not p.startswith("docs/archive/")]

	broken: list[tuple[Path, str]] = []
	for md_file in md_files:
		if not md_file.exists():
			continue
		text = md_file.read_text(encoding="utf-8", errors="replace")
		for link in iter_links(text):
			link_path = normalize_link_path(link)
			if not link_path:
				continue
			target = resolve_target(md_file, link_path)
			try:
				target.relative_to(REPO)
			except ValueError:
				# ignore links that jump outside repo
				continue
			if not target.exists():
				broken.append((md_file, link))

	print(f"non-archive markdown files checked: {len([f for f in md_files if f.exists()])}")
	print(f"broken links found: {len(broken)}")
	for md_file, link in broken:
		rel = md_file.relative_to(REPO)
		print(f"- {rel}: {link}")

	return 0 if not broken else 1


if __name__ == "__main__":
	sys.exit(main())
