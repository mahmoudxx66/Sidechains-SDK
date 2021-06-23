#!/bin/bash

set -euo pipefail

cd /sc-cryptolib 

cargo clean

cargo build -j$(($(nproc)+1)) --release --target=x86_64-unknown-linux-gnu

mkdir -p jni/src/main/resources/native/linux64
cp target/x86_64-unknown-linux-gnu/release/libzendoo_sc.so jni/src/main/resources/native/linux64/libzendoo_sc.so

cd jni
echo "Building jar"
mvn install -P !build-extras -DskipTests=true -Dmaven.javadoc.skip=true -B
echo "Testing jar"
mvn test -P !build-extras -B


