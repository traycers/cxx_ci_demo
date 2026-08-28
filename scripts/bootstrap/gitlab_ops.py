"""GitLab side: wait for readiness, mint a root PAT, create projects, push seed content.

Every repo under repos/<name>/ is pushed branch-by-branch, one subdirectory per branch — see
ADR 0007 for why (independent orphan commits, `main` pushed first, per-branch idempotency).
ADR 0007's rationale is unchanged by the bash-to-Python rewrite; only the mechanics moved.
"""

import os
import random
import shutil
import string
import tempfile
import time

import requests
from git import GitCommandError
from git import Repo
from git.cmd import Git as GitCmd

import config
import docker_ops

log = config.log


def wait_for_gitlab():
    log("waiting for GitLab readiness (this can take several minutes on first boot)...")
    deadline = time.time() + 900  # same ~15 minute budget as the old bash script
    while time.time() < deadline:
        try:
            resp = requests.get(f"{config.GITLAB_URL}/-/readiness", timeout=5)
            if resp.ok:
                log("GitLab is ready.")
                return
        except requests.RequestException:
            pass
        time.sleep(10)
    raise RuntimeError("GitLab did not become ready in time. Check: docker compose logs gitlab")


def create_gitlab_token():
    """Headless PAT for root, scoped for API + git push.

    gitlab-rails runner is the documented headless path; token must be exactly 20 chars.
    'api' scope covers project creation; 'write_repository' explicitly covers git push.
    """
    token = "".join(random.SystemRandom().choice(string.ascii_letters + string.digits) for _ in range(20))
    log("minting a GitLab PAT for root via gitlab-rails runner...")
    script = f"""
        token = User.find_by_username('root').personal_access_tokens.create(
          scopes: ['api', 'write_repository'],
          name: 'ci_cxx bootstrap',
          expires_at: 365.days.from_now
        )
        token.set_token('{token}')
        token.save!
    """
    exit_code, stdout, stderr = docker_ops.exec_in_container("gitlab", ["gitlab-rails", "runner", script])
    if exit_code != 0:
        raise RuntimeError(f"failed to create GitLab PAT — gitlab-rails output:\n{stdout}\n{stderr}")
    return token


def create_gitlab_repo(name, token):
    log(f"creating GitLab project '{name}'...")
    resp = requests.post(
        f"{config.GITLAB_URL}/api/v4/projects",
        headers={"PRIVATE-TOKEN": token},
        data={"name": name, "visibility": "private"},
    )
    if resp.status_code == 201:
        log("  created.")
    elif resp.status_code == 400 and "has already been taken" in resp.text:
        log("  already exists, skipping (bootstrap is safe to re-run).")
    else:
        raise RuntimeError(f"unexpected GitLab API response ({resp.status_code}) creating '{name}': {resp.text}")


def _discover_branches(repo_dir):
    """`main` first (if present), then every other subdirectory, alphabetically.

    `main` first because GitLab makes the first branch ever pushed to an empty repo its default
    branch — pushing it first keeps that default independent of directory-listing order.
    """
    branches = []
    if os.path.isdir(os.path.join(repo_dir, "main")):
        branches.append("main")
    for entry in sorted(os.listdir(repo_dir)):
        full = os.path.join(repo_dir, entry)
        if entry == "main" or not os.path.isdir(full):
            continue
        branches.append(entry)
    return branches


def _branch_exists_on_remote(url, branch):
    try:
        GitCmd().ls_remote("--exit-code", url, branch)
        return True
    except GitCommandError:
        return False


def _push_repo_branch(name, branch, url, src_dir):
    if _branch_exists_on_remote(url, branch):
        log(f"  '{name}' already has a '{branch}' branch on GitLab, skipping push (bootstrap is safe to re-run).")
        return

    tmp = tempfile.mkdtemp()
    try:
        # symlinks=True: copy symlinks as symlinks (matching the old `cp -a`), rather than
        # following them — seed content can carry a stale/broken symlink (e.g. a
        # compile_commands.json pointing at a devcontainer-only build/ path), and the default
        # dereferencing behavior crashes on those instead of just copying the link as-is.
        shutil.copytree(src_dir, tmp, dirs_exist_ok=True, symlinks=True)
        repo = Repo.init(tmp)
        repo.git.checkout("-b", branch)
        repo.git.add(A=True)
        env = {
            "GIT_AUTHOR_NAME": config.GIT_AUTHOR_NAME,
            "GIT_AUTHOR_EMAIL": config.GIT_AUTHOR_EMAIL,
            "GIT_COMMITTER_NAME": config.GIT_AUTHOR_NAME,
            "GIT_COMMITTER_EMAIL": config.GIT_AUTHOR_EMAIL,
        }
        with repo.git.custom_environment(**env):
            repo.git.commit("-m", "initial content from ci_cxx bootstrap")
        origin = repo.create_remote("origin", url)
        log(f"pushing '{name}' branch '{branch}' to GitLab...")
        origin.push(branch)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


def push_repo_content(name, token):
    """Push repos/<name>/<branch>/ content into each repo, one branch per subdirectory.

    Each branch is its own independent orphan commit (fresh init, single commit) — the
    directories hold deliberately different content, not a fork of one codebase, so a shared
    root would carry no meaning. See ADR 0007.
    """
    repo_dir = os.path.join(config.REPOS_DIR, name)
    if not os.path.isdir(repo_dir):
        raise RuntimeError(f"{repo_dir} does not exist — was the image built with repos/ copied in?")

    url = f"http://root:{token}@{config.GITLAB_HOST}:{config.GITLAB_HTTP_PORT}/root/{name}.git"

    for branch in _discover_branches(repo_dir):
        _push_repo_branch(name, branch, url, os.path.join(repo_dir, branch))
