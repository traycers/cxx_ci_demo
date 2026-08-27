🇬🇧 English · [🇷🇺 Русский](../../ru/adr/0002-no-registry-shared-docker-daemon.md) · [🇨🇳 中文](../../zh/adr/0002-no-registry-shared-docker-daemon.md)

# No Docker registry — root image build stays in the shared host Docker daemon

There is exactly one TeamCity build agent, and it shares the host's `docker.sock` for every container it runs. A normal CI setup would push the image built by the root build to a registry so other agents/hosts could pull it.

We chose to skip a registry entirely: the root image build's `docker build` lands directly in the one shared docker daemon, and downstream C++ project builds reference it by tag (`%build_image_cxx%`) via `docker run` — no push/pull step.

Consequence: this only works because all builds share a single docker daemon. Adding a second build agent (horizontal scaling) would require introducing a registry — a known, deliberate limitation of this demo setup, not an oversight.

Corollary once releases exist (see `docs/en/adding-a-release.md`): every release's root image build lands in that same one shared daemon, so a bare `%build.number%` tag isn't enough to keep them apart — two releases could each produce a build numbered, say, 12. Each release's `BuildCImage` tags its image `cxxci-build:<config_name>-%build.number%` (e.g. `cxxci-build:main-106`), and every downstream build type in that release constructs the same prefixed tag to consume it. Still no registry, still one daemon — just a wider tag namespace.

Second corollary: no registry also means no registry-side garbage collection — every image any release ever built sits in the shared daemon forever unless something deletes it. Confirmed live: 104 stray `cxxci-build:*` tags accumulated on the demo host before this was addressed. `BuildCImage`'s "cleanup old images" step (runs only after a successful "docker build") keeps the `%keep_images_count%` newest tags for its own release's prefix and `docker rmi -f`s the rest — each release prunes only its own tags, never touches another release's images.
