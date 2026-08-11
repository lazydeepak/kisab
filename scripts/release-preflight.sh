#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repository_root"

if [ "$#" -gt 1 ]; then
  echo "usage: scripts/release-preflight.sh [annotated-version-tag]" >&2
  exit 2
fi

if ! command -v actionlint >/dev/null 2>&1; then
  echo "release preflight requires actionlint on PATH" >&2
  exit 1
fi

if [ -n "$(git status --porcelain)" ]; then
  echo "release preflight requires a clean worktree" >&2
  exit 1
fi

actionlint
./gradlew :app:verifyReleaseMetadata :app:verifyLocal

version_name="$(./gradlew -q :app:printVersionInfo | sed -n 's/^versionName=//p')"
version_code="$(./gradlew -q :app:printVersionInfo | sed -n 's/^versionCode=//p')"
if [ -z "$version_name" ] || [ -z "$version_code" ]; then
  echo "could not read Android version metadata" >&2
  exit 1
fi

release_tag="${1:-}"
if [ -n "$release_tag" ]; then
  if [ "$release_tag" != "v${version_name}" ]; then
    echo "tag '$release_tag' does not match versionName 'v${version_name}'" >&2
    exit 1
  fi
  if [ "$(git cat-file -t "$release_tag" 2>/dev/null || true)" != "tag" ]; then
    echo "tag '$release_tag' must exist locally and be annotated" >&2
    exit 1
  fi
  release_commit="$(git rev-parse "${release_tag}^{}")"
  git fetch --no-tags origin main
  if ! git merge-base --is-ancestor "$release_commit" refs/remotes/origin/main; then
    echo "tag '$release_tag' is not contained in origin/main" >&2
    exit 1
  fi
fi

echo "release preflight passed: versionName=${version_name} versionCode=${version_code}${release_tag:+ tag=${release_tag}}"
echo "evidence: app/build/reports/verification/local-ci-evidence.json"
