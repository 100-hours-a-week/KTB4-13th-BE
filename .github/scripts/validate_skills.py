#!/usr/bin/env python3

import re
import sys
from pathlib import Path

import yaml

MAX_SKILL_NAME_LENGTH = 64
ALLOWED_PROPERTIES = {"name", "description", "license", "allowed-tools", "metadata", "argument-hint", "disable-model-invocation"}


def validate_skill(skill_md: Path):
    content = skill_md.read_text(encoding="utf-8")
    if not content.startswith("---"):
        return False, "No YAML frontmatter found"

    match = re.match(r"^---\n(.*?)\n---", content, re.DOTALL)
    if not match:
        return False, "Invalid frontmatter format"

    try:
        frontmatter = yaml.safe_load(match.group(1))
    except yaml.YAMLError as error:
        return False, f"Invalid YAML in frontmatter: {error}"

    if not isinstance(frontmatter, dict):
        return False, "Frontmatter must be a YAML dictionary"

    if any(not isinstance(key, str) for key in frontmatter):
        return False, "Frontmatter keys must be strings"

    unexpected_keys = set(frontmatter) - ALLOWED_PROPERTIES
    if unexpected_keys:
        unexpected = ", ".join(sorted(unexpected_keys))
        return False, f"Unexpected key(s) in SKILL.md frontmatter: {unexpected}"

    if "name" not in frontmatter:
        return False, "Missing 'name' in frontmatter"
    if "description" not in frontmatter:
        return False, "Missing 'description' in frontmatter"

    name = frontmatter["name"]
    if not isinstance(name, str):
        return False, f"Name must be a string, got {type(name).__name__}"
    name = name.strip()
    if not name:
        return False, "Name cannot be empty"
    if not re.match(r"^[a-z0-9-]+$", name):
        return False, f"Name '{name}' should be hyphen-case"
    if name.startswith("-") or name.endswith("-") or "--" in name:
        return False, f"Name '{name}' cannot start/end with hyphen or contain consecutive hyphens"
    if len(name) > MAX_SKILL_NAME_LENGTH:
        return False, f"Name is too long ({len(name)} characters)"

    description = frontmatter["description"]
    if not isinstance(description, str):
        return False, f"Description must be a string, got {type(description).__name__}"
    description = description.strip()
    if not description:
        return False, "Description cannot be empty"
    if "<" in description or ">" in description:
        return False, "Description cannot contain angle brackets (< or >)"
    if len(description) > 1024:
        return False, f"Description is too long ({len(description)} characters)"

    return True, "Skill is valid!"


def main():
    skill_files = sorted(Path(".agents/skills").glob("*/SKILL.md"))
    if not skill_files:
        print("No project skills found")
        return 1

    failed = False
    for skill_md in skill_files:
        valid, message = validate_skill(skill_md)
        print(f"{skill_md.parent}: {message}")
        failed |= not valid

    return int(failed)


if __name__ == "__main__":
    sys.exit(main())
