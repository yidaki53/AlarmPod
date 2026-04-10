import json
import os
import re
import subprocess
import urllib.error
import urllib.parse
import urllib.request


LABEL_MAJOR = {
    "breaking",
    "breaking-change",
    "major",
    "release:major",
    "semver:major",
}
LABEL_MINOR = {
    "enhancement",
    "feature",
    "minor",
    "release:minor",
    "semver:minor",
}
LABEL_PATCH = {
    "bug",
    "bugfix",
    "fix",
    "patch",
    "release:patch",
    "semver:patch",
}
BUMP_ORDER = {"none": 0, "patch": 1, "minor": 2, "major": 3}


def run_git(*args: str) -> str:
    return subprocess.check_output(["git", *args], text=True).strip()


def parse_version(version: str) -> tuple[int, int, int]:
    match = re.fullmatch(r"v?(\d+)\.(\d+)\.(\d+)", version)
    if match is None:
        raise ValueError(f"Unsupported version tag: {version}")
    return tuple(int(group) for group in match.groups())


def format_version(version: tuple[int, int, int]) -> str:
    major, minor, patch = version
    return f"{major}.{minor}.{patch}"


def bump_version(version: tuple[int, int, int], bump: str) -> tuple[int, int, int]:
    major, minor, patch = version
    if bump == "major":
        return major + 1, 0, 0
    if bump == "minor":
        return major, minor + 1, 0
    if bump == "patch":
        return major, minor, patch + 1
    return version


def stable_version_code(version: tuple[int, int, int]) -> int:
    major, minor, patch = version
    return major * 1_000_000 + minor * 10_000 + patch * 100 + 95


def current_app_version() -> tuple[int, int, int]:
    with open("app/build.gradle", encoding="utf-8") as build_file:
        content = build_file.read()
    match = re.search(r'versionName\s*=\s*overriddenVersionName != null \? overriddenVersionName\.toString\(\) : "([0-9]+\.[0-9]+\.[0-9]+)"', content)
    if match is None:
        raise ValueError("Could not determine the current app version from app/build.gradle")
    return parse_version(match.group(1))


def latest_tag() -> str | None:
    tags = run_git("tag", "--list", "v*", "--sort=-v:refname").splitlines()
    for tag in tags:
        if re.fullmatch(r"v\d+\.\d+\.\d+", tag):
            return tag
    return None


def commit_range(base_tag: str | None) -> str:
    if base_tag is None:
        return "HEAD"
    return f"{base_tag}..HEAD"


def load_commits(git_range: str) -> list[dict[str, str]]:
    raw = run_git("log", "--pretty=format:%H%x1f%s%x1f%b%x1e", git_range)
    commits = []
    for entry in raw.split("\x1e"):
        if not entry.strip():
            continue
        sha, subject, body = (entry.split("\x1f") + [""])[:3]
        commits.append({"sha": sha, "subject": subject, "body": body})
    return commits


def load_recent_commits(limit: int) -> list[dict[str, str]]:
    raw = run_git("log", f"-n{limit}", "--pretty=format:%H%x1f%s%x1f%b%x1e", "HEAD")
    commits = []
    for entry in raw.split("\x1e"):
        if not entry.strip():
            continue
        sha, subject, body = (entry.split("\x1f") + [""])[:3]
        commits.append({"sha": sha, "subject": subject, "body": body})
    return commits


def github_json(url: str) -> list[dict] | dict:
    token = os.environ.get("GITHUB_TOKEN")
    headers = {
        "Accept": "application/vnd.github+json",
        "User-Agent": "compute-next-release",
    }
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(request) as response:
        return json.loads(response.read().decode("utf-8"))


def pr_labels_for_commit(repository: str, sha: str) -> list[str]:
    encoded_repository = urllib.parse.quote(repository, safe="/")
    encoded_sha = urllib.parse.quote(sha, safe="")
    url = f"https://api.github.com/repos/{encoded_repository}/commits/{encoded_sha}/pulls"
    try:
        pulls = github_json(url)
    except urllib.error.HTTPError:
        return []
    labels: list[str] = []
    if isinstance(pulls, list):
        for pull in pulls:
            for label in pull.get("labels", []):
                name = label.get("name")
                if isinstance(name, str):
                    labels.append(name.lower())
    return labels


def bump_from_labels(labels: list[str]) -> str:
    label_set = set(labels)
    if label_set & LABEL_MAJOR:
        return "major"
    if label_set & LABEL_MINOR:
        return "minor"
    if label_set & LABEL_PATCH:
        return "patch"
    return "none"


def bump_from_commit_message(subject: str, body: str) -> str:
    message = f"{subject}\n{body}"
    if "BREAKING CHANGE" in message or re.match(r"^[a-z]+(\([^\)]+\))?!:", subject):
        return "major"

    conventional_match = re.match(r"^([a-z]+)(\([^\)]+\))?:", subject)
    if conventional_match is None:
        return "none"

    commit_type = conventional_match.group(1)
    if commit_type == "feat":
        return "minor"
    if commit_type in {"fix", "perf", "refactor", "revert"}:
        return "patch"
    return "none"


def strongest_bump(bumps: list[str], default_bump: str) -> str:
    highest = default_bump if default_bump in BUMP_ORDER else "none"
    for bump in bumps:
        if BUMP_ORDER[bump] > BUMP_ORDER[highest]:
            highest = bump
    return highest


def write_output(name: str, value: str) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT")
    if output_path:
        with open(output_path, "a", encoding="utf-8") as output_file:
            output_file.write(f"{name}={value}\n")
    print(f"{name}={value}")


def main() -> None:
    repository = os.environ.get("GITHUB_REPOSITORY", "")
    default_bump = os.environ.get("DEFAULT_BUMP", "patch").lower()
    base_tag = latest_tag()
    if base_tag is None:
        base_version = current_app_version()
        commits = load_recent_commits(100)
    else:
        base_version = parse_version(base_tag)
        git_range = commit_range(base_tag)
        commits = load_commits(git_range)
    if not commits:
        raise SystemExit("No commits found since the latest release tag.")

    bumps: list[str] = []
    for commit in commits:
        labels = pr_labels_for_commit(repository, commit["sha"]) if repository else []
        label_bump = bump_from_labels(labels)
        if label_bump != "none":
            bumps.append(label_bump)
            continue
        bumps.append(bump_from_commit_message(commit["subject"], commit["body"]))

    release_bump = strongest_bump(bumps, default_bump)
    should_release = release_bump != "none"

    write_output("base_tag", base_tag or "")
    write_output("bump", release_bump)
    write_output("should_release", str(should_release).lower())
    if not should_release:
        return

    next_version = bump_version(base_version, release_bump)
    version_string = format_version(next_version)
    version_code = stable_version_code(next_version)

    write_output("version", version_string)
    write_output("version_code", str(version_code))


if __name__ == "__main__":
    main()