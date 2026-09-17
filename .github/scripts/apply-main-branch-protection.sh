#!/usr/bin/env bash
set -euo pipefail

repository="${1:-$(gh repo view --json nameWithOwner --jq '.nameWithOwner')}"
branch="${2:-main}"

if [[ "$branch" != "main" ]]; then
    echo "main 브랜치만 보호할 수 있습니다." >&2
    exit 1
fi

gh api \
    --method PUT \
    --header 'Accept: application/vnd.github+json' \
    "repos/${repository}/branches/${branch}/protection" \
    --input - <<'JSON'
{
  "required_status_checks": {
    "strict": true,
    "contexts": [
      "Gradle check",
      "Git conventions"
    ]
  },
  "enforce_admins": true,
  "required_pull_request_reviews": {
    "dismiss_stale_reviews": true,
    "require_code_owner_reviews": true,
    "required_approving_review_count": 1
  },
  "restrictions": null
}
JSON
