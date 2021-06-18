## Build env_dependencies
FROMopenjdk:11 AS simpleapp-builder

SHELL ["/bin/bash", "-c"]

ARG RUSTUP_TOOLCHAIN=1.51.0
ARG SDK_TAG=0.2.7
ENV PATH="/root/.cargo/bin:${PATH}"

RUN set -euxo pipefail \
    && export DEBIAN_FRONTEND=noninteractive \
    && apt-get update \
    && apt-get -y --no-install-recommends install apt-utils \
    && apt-get -y --no-install-recommends dist-upgrade \
    && apt-get -y --no-install-recommends install build-essential ca-certificates cmake curl git scala maven gcc-mingw-w64-x86-64 \
    && git clone --depth 1 --branch ${SDK_TAG} https://github.com/HorizenOfficial/Sidechains-SDK.git /Sidechains-SDK \
    && cd /Sidechains-SDK && mvn install -DskipTests \
    && curl https://sh.rustup.rs -sSf | bash -s -- -y \
    && rustup target add x86_64-pc-windows-gnu

# split into two RUN commands to make use of build cache in previous layer
COPY . /simpleapp

RUN set -euxo pipefail \
    && cd /simpleapp && mvn install

## Build Final Image
FROM openjdk:11-jre-slim

SHELL ["/bin/bash", "-c"]

WORKDIR /simpleapp

COPY --from=simpleapp-builder /simpleapp/target/*.jar /Sidechains-SDK/tools/sctool/target/sidechains-sdk-scbootstrappingtools-*.jar /simpleapp/
COPY --from=simpleapp-builder /simpleapp/target/lib /simpleapp/lib
COPY ./ci/entrypoint.sh ./src/main/resources/* /simpleapp/config/

RUN set -euxo pipefail \
    && mv /simpleapp/config/entrypoint.sh /usr/local/bin/entrypoint.sh \
    && chmod 755 /usr/local/bin/entrypoint.sh \
    && mkdir -p /simpleapp/tools \
    && mv /simpleapp/sidechains-sdk-scbootstrappingtools-*.jar /simpleapp/tools/ \
    && export DEBIAN_FRONTEND=noninteractive \
    && apt-get update \
    && apt-get -y --no-install-recommends install apt-utils \
    && apt-get -y --no-install-recommends dist-upgrade \
    && apt-get -y --no-install-recommends install ca-certificates curl dnsutils gosu tini wget \
    && wget "http://ftp.us.debian.org/debian/pool/main/j/jemalloc/libjemalloc1_3.6.0-9.1_amd64.deb" \
    && dpkg -i libjemalloc1_3.6.0-9.1_amd64.deb \
    && apt-get -y clean && apt-get -y autoclean \
    && rm -rf /var/{lib/apt/lists/*,cache/apt/archives/*.deb,tmp/*,log/*} /tmp/* \

EXPOSE 9084

ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/entrypoint.sh"]

CMD ["java", "-cp", "/simpleapp/sidechains-sdk-simpleapp-0.2.7.jar:/simpleapp/lib/*", "com.horizen.examples.SimpleApp", "/simpleapp/config/simpleapp_settings.conf"]

