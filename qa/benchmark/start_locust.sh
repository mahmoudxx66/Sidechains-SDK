#!/bin/bash
BASEDIR=$(pwd);
FIXTURE_DIR="${BASEDIR}/fixtures";
FIXTURE_DATA_CREATION_DIR="${FIXTURE_DIR}/dataCreation";
LOCUST_DIR="${BASEDIR}/bleth"

WALLET_FILE="${LOCUST_DIR}/wallets.json"
CONTRACT_FILE="${LOCUST_DIR}/token.json"
TRANSACTIONS_FILE="${LOCUST_DIR}/tx_out.json"

[ -z "$1" ] && echo "No argument supplied, please supply a tag to execute from the list [ estimategas, gasprice, unclecount, txcountbynumber, txcount, getcode, getstorageat, getbalance, getblockbynumber, getlogs, gasprice, txbyhash ]" && exit 1;

if [ ! -f "${WALLET_FILE}" ]; then
    echo "${WALLET_FILE} not found... generate the data by running benchmark.sh" && exit 1;
fi

if [ ! -f "${CONTRACT_FILE}" ]; then
    echo "${CONTRACT_FILE} not found... generate the data by running benchmark.sh" && exit 1;
fi

if [ ! -f "${TRANSACTIONS_FILE}" ]; then
    echo "${TRANSACTIONS_FILE} not found... generate the data by running benchmark.sh" && exit 1;
fi

echo "preparing the benchmark...";
HOST=$(expr substr $(grep "NGINX_HTPASSWD=" "${FIXTURE_DATA_CREATION_DIR}/.env") 16 64)
echo "building locust...";
cd "$LOCUST_DIR" || exit 1;
./build.sh || exit 1;
echo "starting locust with tag ${1}...";
./locust.sh "${HOST}@127.0.0.1/dev1/ethv1" "${1}"