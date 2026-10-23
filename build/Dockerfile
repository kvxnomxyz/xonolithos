FROM ubuntu:22.04
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
    bc bison build-essential ca-certificates ccache curl erofs-utils file flex \
    g++-multilib gcc-multilib git git-lfs gnupg gperf imagemagick kmod \
    lib32readline-dev lib32z1-dev libdw-dev libelf-dev libgnutls28-dev \
    liblz4-tool libncurses5 libsdl1.2-dev libssl-dev libxml2 libxml2-utils \
    lzop openssh-client patch pngcrush protobuf-compiler python3 python3-protobuf \
    rsync schedtool squashfs-tools unzip xxd xsltproc zip zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*
# repo itself verifies its signed release before running downloaded implementation.
RUN curl --fail --location --retry 3 https://storage.googleapis.com/git-repo-downloads/repo \
    -o /usr/local/bin/repo && chmod 755 /usr/local/bin/repo
ARG BUILDER_UID=1000
ARG BUILDER_GID=1000
RUN groupadd --gid "$BUILDER_GID" xos && \
    useradd --uid "$BUILDER_UID" --gid "$BUILDER_GID" --create-home --shell /bin/bash xos
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
WORKDIR /workspace
ENTRYPOINT ["bash", "/workspace/scripts/build-rom.sh"]
