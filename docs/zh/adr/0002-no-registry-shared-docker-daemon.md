[🇬🇧 English](../../en/adr/0002-no-registry-shared-docker-daemon.md) · [🇷🇺 Русский](../../ru/adr/0002-no-registry-shared-docker-daemon.md) · 🇨🇳 中文

_翻译自 `docs/en/adr/0002-no-registry-shared-docker-daemon.md`。原文变更时请同步更新本文件——参见 [ADR 0006](0006-trilingual-docs-mirror-tree.md)。_

# 不使用 Docker registry——根镜像构建停留在共享的宿主机 Docker 守护进程中

只有一个 TeamCity build agent，它为运行的每一个容器共享宿主机的 `docker.sock`。正常的 CI 配置会把根构建产生的镜像推送到某个 registry，以便其他 agent/主机拉取。

我们选择完全跳过 registry：根镜像构建的 `docker build` 直接落在同一个共享 docker 守护进程里，下游 C++ 项目构建通过 `docker run` 按标签(`%build_image_cxx%`)引用它——没有 push/pull 步骤。

结论：这只有在所有构建共享同一个 docker 守护进程的前提下才成立。添加第二个 build agent(水平扩展)就需要引入 registry——这是这个演示环境已知且有意接受的限制，不是疏漏。

有了 release 之后的推论(见 `docs/zh/adding-a-release.md`)：每个 release 的根镜像构建都落在同一个共享守护进程里，所以单纯的 `%build.number%` 标签不足以把它们区分开——两个 release 各自都可能产生编号同为，比如说，12 的构建。每个 release 的 `BuildCImage` 都会把镜像标记为 `cxxci-build:<config_name>-%build.number%`(例如 `cxxci-build:main-106`)，该 release 中每一个下游 build type 都构造相同的带前缀标签来消费它。仍然没有 registry，仍然是同一个守护进程——只是标签命名空间更宽了。

第二个推论：没有 registry 也意味着没有 registry 一侧的垃圾回收——任何 release 曾经构建过的任何镜像都会永远留在共享守护进程里，除非有东西显式删除它。已在真实环境中确认：在这一点被解决之前，demo 主机上积累了 104 个孤儿 `cxxci-build:*` 标签。`BuildCImage` 的「清理旧镜像」步骤(只在 "docker build" 成功之后执行)会为自己 release 的前缀保留 `%keep_images_count%` 个最新标签，并用 `docker rmi -f` 删除其余的——每个 release 只清理自己的标签，绝不会碰到另一个 release 的镜像。
