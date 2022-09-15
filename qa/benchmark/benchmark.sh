#!/bin/bash
BASEDIR=$(pwd);
EVM_NODE_DIR="${BASEDIR}/evmNodeEnvironment";
NODE_ENV_DIR="${EVM_NODE_DIR}/compose-evm-regtest-v1";
FIXTURE_DIR="${BASEDIR}/fixtures";
FIXTURE_DATA_CREATION_DIR="${FIXTURE_DIR}/dataCreation";
WALLET_FILE="${FIXTURE_DIR}/wallets.json"
CONTRACT_FILE="${FIXTURE_DIR}/token.json"
TRANSACTIONS_FILE="${FIXTURE_DIR}/tx_out.json"
LOCUST_DIR="${BASEDIR}/bleth"

WALLET_AMOUNT="1000"

echo "fetching node repo and launch the nodes...";
cd "$EVM_NODE_DIR" || exit;
git clone "git@github.com:rocknitive/compose-evm-regtest-v1.git";
echo "attempting to check out single forger branch...";
git checkout -b cr/single_forger
echo "launching the nodes...";
cd "${NODE_ENV_DIR}/scripts" && printf '%s\n' y | ./init.sh || exit;
cp "${NODE_ENV_DIR}/.env" "${FIXTURE_DATA_CREATION_DIR}/.env"

# docker network ls

echo "creating the fixtures...";
cd "$FIXTURE_DATA_CREATION_DIR" || exit 1;

echo "installing hardhat deps...";
yarn install --non-interactive --frozen-lockfile;

echo "creating 1K accounts...";
npx hardhat gen-wallets --amount "${WALLET_AMOUNT}" --out-file "${WALLET_FILE}"

echo "checking rpc basic provider functionality...";
npx hardhat check-provider --network evm-benchmark || exit 1;

echo "deploying ERC-20 contract...";
npx hardhat deploy --network evm-benchmark --name "EVMTestToken" --symbol "ETST" --type "erc20" --out-file "${CONTRACT_FILE}" || exit 1;

# echo "creating gas tokens and distribute them..."; // ?

echo "sending tokens to addresses...";
npx hardhat send-initial-tokens --network evm-benchmark --token-file "${CONTRACT_FILE}" --wallet-file "${WALLET_FILE}" --amount 100 --out-file "${TRANSACTIONS_FILE}" || exit 1;

echo "starting the benchmark...";
echo "locust...";
cd "$LOCUST_DIR" || exit 1;
./build.sh
./locust.sh "${process.env.NGINX_HTPASSWD}@127.0.0.1/dev1/ethv1"