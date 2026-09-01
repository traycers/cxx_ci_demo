#!/usr/bin/env bash
# Creates a new track (see docs/adding-a-track.md) by copying an existing one and
# mechanically renaming everything the copy needs renamed — this exists because doing that by
# hand is genuinely error-prone: writing the procedure down and following it once already
# produced the exact mistake documented as a warning below (a doubled id prefix), caught only by
# pushing to a live server and reading the compiled result.
#
# Usage: scripts/new-track.sh <new_track_name> [source_track_name]
#   new_track_name     required. snake_case, becomes the track's directory name, TeamCity
#                        project name, git branch name, and docker image tag prefix.
#   source_track_name  optional, default "main". Must be an existing cxx_ci_demo/<name>/
#                        directory — copy from any track, not just main; the rename logic
#                        doesn't care which one, they're all shaped the same way.
#
# What it does NOT do (still yours to do — see the printed next-steps at the end):
#   - Push the result to the live ci-infra repo, or run bootstrap.sh
#   - Create the actual refs/heads/<new_track_name> branch in project_a through project_e on GitLab
#   - Edit the new Dockerfile if this track needs a different toolchain/base image
set -euo pipefail

NEW_TRACK_NAME="${1:?usage: scripts/new-track.sh <new_track_name> [source_track_name]}"
SOURCE_TRACK_NAME="${2:-main}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CXX_CI_DEMO_DIR="${REPO_ROOT}/repos/ci-infra/main/.teamcity/cxx_ci_demo"
INDEX_FILE="${CXX_CI_DEMO_DIR}/CxxCiDemo.kt"

SRC_DIR="${CXX_CI_DEMO_DIR}/${SOURCE_TRACK_NAME}"
DST_DIR="${CXX_CI_DEMO_DIR}/${NEW_TRACK_NAME}"

[ -d "$SRC_DIR" ] || { echo "error: source track '${SOURCE_TRACK_NAME}' not found at ${SRC_DIR}" >&2; exit 1; }
[ -e "$DST_DIR" ] && { echo "error: '${NEW_TRACK_NAME}' already exists at ${DST_DIR}" >&2; exit 1; }
[ -f "$INDEX_FILE" ] || { echo "error: ${INDEX_FILE} not found" >&2; exit 1; }

# snake_case -> PascalCase (track_2_0 -> Track20). The one place this conversion is defined —
# docs/adding-a-track.md's examples must match what this function actually produces.
to_pascal_case() {
    awk -F'_' '{ for (i = 1; i <= NF; i++) $i = toupper(substr($i, 1, 1)) substr($i, 2); print }' OFS='' <<<"$1"
}

SOURCE_WORD="$(to_pascal_case "$SOURCE_TRACK_NAME")"
NEW_WORD="$(to_pascal_case "$NEW_TRACK_NAME")"

SRC_PROJECT_FILE="${SRC_DIR}/${SOURCE_WORD}.kt"
[ -f "$SRC_PROJECT_FILE" ] || { echo "error: expected project file ${SRC_PROJECT_FILE} not found — is '${SOURCE_TRACK_NAME}' really a track directory?" >&2; exit 1; }

echo "copying ${SRC_DIR#"$REPO_ROOT"/} -> ${DST_DIR#"$REPO_ROOT"/}"
cp -r "$SRC_DIR" "$DST_DIR"

# Rename every file that declares a top-level `val` (Kotlin wraps these in a synthetic class
# named after the *file*, and nothing under .teamcity/ declares a package — see IdPath.kt — so
# two same-named files anywhere in the tree collide at compile time). This used to be just the
# one top-level project file (Main.kt -> Track20.kt); a track can now also nest package-variant
# subprojects with their own top-level-val project file (e.g. debug/MainDebug.kt,
# release/MainRelease.kt) — every one of those needs the same treatment, not just the top-level
# one, so find them all rather than hardcoding a single mv.
while IFS= read -r -d '' f; do
    base="$(basename "$f")"
    case "$base" in
        "${SOURCE_WORD}"*.kt)
            mv "$f" "$(dirname "$f")/${NEW_WORD}${base#"$SOURCE_WORD"}"
            ;;
        *)
            echo "error: found a top-level-val file whose name doesn't start with '${SOURCE_WORD}' (${f#"$REPO_ROOT"/}) — new-track.sh doesn't know how to rename it safely, aborting" >&2
            exit 1
            ;;
    esac
done < <(grep -rlZ '^val ' "$DST_DIR" --include='*.kt')

# Rename identifiers. Order matters: the *Id/*TrackName suffixed forms first (so the generic
# "<SourceWord>_" prefix pass below doesn't also have to worry about them), longest-match first
# throughout so e.g. a Vcs-suffixed object isn't partially caught by a shorter pattern first.
# This only touches Kotlin identifiers made of <SourceWord> plus a suffix/prefix — it does NOT
# touch the plain leaf words passed as string literals into IdPath composition
# (id((MainId / "ProjectA").toString())) because those strings don't contain SOURCE_WORD as
# a substring, only the *object* names being renamed do.
find "$DST_DIR" -name '*.kt' -print0 | xargs -0 sed -i \
    -e "s/\\b${SOURCE_WORD}Id\\b/${NEW_WORD}Id/g" \
    -e "s/\\b${SOURCE_WORD}TrackName\\b/${NEW_WORD}TrackName/g" \
    -e "s/\\b${SOURCE_WORD}_/${NEW_WORD}_/g" \
    -e "s/\\b${SOURCE_WORD}\\b/${NEW_WORD}/g"

# Rename the track's own data (not identifiers): the TrackName val's string value, the
# project's name/description, branch_default, and both branch_spec alternatives. All of these
# are the literal word SOURCE_TRACK_NAME (snake_case) appearing inside quotes — word-bounded so
# this can't clip a substring match (e.g. "main" won't touch "domain" or "maintain": no word
# boundary between the shared letters).
find "$DST_DIR" -name '*.kt' -print0 | xargs -0 sed -i \
    -e "s/\\b${SOURCE_TRACK_NAME}\\b/${NEW_TRACK_NAME}/g"

# Sanity check: a doubled prefix (CxxCiDemo_Word_Word_Thing) means an id(...) string literal got
# clobbered — this exact mistake happened once already doing this by hand. Fail loudly instead
# of pushing something broken.
if grep -rn "\"${NEW_WORD}_${NEW_WORD}_\|\"${NEW_WORD}_.*${NEW_WORD}_" "$DST_DIR" >/dev/null 2>&1; then
    echo "error: doubled prefix detected in a string literal — aborting, nothing was left half-renamed outside ${DST_DIR}:" >&2
    grep -rn "\"${NEW_WORD}_" "$DST_DIR" >&2
    exit 1
fi

# Register the new track. Insert right before the index project's closing "})" so this works
# regardless of how many tracks are already registered.
sed -i "s/^})\$/    subProject(${NEW_WORD})\\n})/" "$INDEX_FILE"

echo "done."
echo
echo "Next steps:"
echo "  1. Review the diff, especially ${DST_DIR#"$REPO_ROOT"/}/${NEW_WORD}.kt (branch_default/branch_spec/description)."
echo "  2. If this track needs a different build environment, edit ${DST_DIR#"$REPO_ROOT"/}/Dockerfile."
echo "  3. Push repos/ci-infra/main/.teamcity to the live ci-infra repo's main branch."
echo "  4. Run bootstrap.sh once so it injects the GitLab credential into ${NEW_WORD}'s VCS roots"
echo "     (it currently only loops over the main track's — see bootstrap.sh's provision_teamcity)."
echo "  5. Create refs/heads/${NEW_TRACK_NAME} in project_a through project_e on GitLab."
