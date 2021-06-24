## Build env_dependencies
FROM zencash/sc-ci-base:bionic_rust-1.51.0_jdk-11_latest AS simpleapp-builder

SHELL ["/bin/bash", "-c"]

ENV PATH="/root/.cargo/bin:${PATH}"
COPY ./ci/build_sc_cryptolib.sh /tmp/build_sc_cryptolib.sh

RUN set -euxo pipefail \
    && export DEBIAN_FRONTEND=noninteractive \
    && apt-get update \
    && apt-get -y --no-install-recommends install apt-utils \
    && apt-get -y --no-install-recommends dist-upgrade \
    && apt-get -y --no-install-recommends install cmake git gnupg1-curl \
    && git clone --depth 1 --branch development_tmp https://github.com/HorizenOfficial/zendoo-sc-cryptolib.git /sc-cryptolib \
    && mv /tmp/build_sc_cryptolib.sh /sc-cryptolib/ && chmod +x /sc-cryptolib/build_sc_cryptolib.sh && ./sc-cryptolib/build_sc_cryptolib.sh \
    && git clone --depth 1 --branch dev https://github.com/HorizenOfficial/Sidechains-SDK.git /Sidechains-SDK \
    && cd /Sidechains-SDK && mvn install -DskipTests

# split into two RUN commands to make use of build cache in previous layer

## Build Final Image
FROM zencash/sc-ci-base:bionic_jdk-11_latest
ENV TINI_VERSION=v0.19.0
SHELL ["/bin/bash", "-c"]

WORKDIR /simpleapp

COPY --from=simpleapp-builder /Sidechains-SDK/examples/simpleapp/target/*.jar /Sidechains-SDK/tools/sctool/target/sidechains-sdk-scbootstrappingtools-*.jar /simpleapp/
COPY --from=simpleapp-builder /Sidechains-SDK/examples/simpleapp/target/lib /simpleapp/lib
COPY --from=simpleapp-builder /Sidechains-SDK/tools/sctool/target/lib simpleapp/bottstrappingtools_lib
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
    && apt-get -y --no-install-recommends install dnsutils libjemalloc1 \
    && wget -O /usr/bin/tini-static-amd64 "https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-static-amd64" \
    && wget -O /usr/bin/tini-static-amd64.asc "https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini-static-amd64.asc" \
    && export GNUPGHOME="$(mktemp -d)" \
    && ( gpg2 --batch --keyserver keyserver.pgp.com --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 || \
       gpg2 --batch --keyserver pgp.key-server.io --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 || \
       gpg2 --batch --keyserver keyserver.pgp.com --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 || \
       gpg2 --batch --keyserver pgp.key-server.io --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 || \
       gpg2 --batch --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 ) \
    && gpg2 --batch --verify /usr/bin/tini-static-amd64.asc /usr/bin/tini-static-amd64 \
    && chmod +x /usr/bin/tini-static-amd64 && mv /usr/bin/tini-static-amd64 /usr/bin/tini \
    && rm -rf  rm -rf "$GNUPGHOME" /usr/local/bin/tini-static-amd64.asc \
    && apt-get -y clean && apt-get -y autoclean \
    && rm -rf /var/{lib/apt/lists/*,cache/apt/archives/*.deb,tmp/*,log/*} /tmp/*

EXPOSE 9084

ENTRYPOINT ["/usr/bin/tini", "--", "/usr/local/bin/entrypoint.sh"]

CMD ["java", "-cp", "/simpleapp/sidechains-sdk-simpleapp-0.2.7.jar:/simpleapp/lib/*", "com.horizen.examples.SimpleApp", "/simpleapp/config/simpleapp_settings.conf"]
