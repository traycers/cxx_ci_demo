FROM ubuntu:24.04

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
        build-essential \
        cmake \
        ninja-build \
        ca-certificates \
        libgtest-dev \
        libgmock-dev \
    && rm -rf /var/lib/apt/lists/*
