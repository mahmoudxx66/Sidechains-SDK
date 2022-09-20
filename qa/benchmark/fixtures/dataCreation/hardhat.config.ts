import * as dotenv from "dotenv";

import { HardhatUserConfig } from "hardhat/config";
import "@typechain/hardhat";
import "@typechain/ethers-v5";
import "solidity-coverage";
import "./tasks/deploy";
import "./tasks/gen-wallets";
import "./tasks/check-provider";
import "./tasks/send-initial-tokens";

dotenv.config();

const networks: any = {};
if (process.env.SCNODE_WALLET_SEED_DEV1) {
  const rpcurl = `http://${process.env.NGINX_HTPASSWD}@127.0.0.1/dev1/ethv1`;
  networks["evm-benchmark"] = {
    url: rpcurl,
    accounts: [process.env.SCNODE_WALLET_GENESIS_SECRETS_DEV1.substring(3, 67)],
  };
}

const config: HardhatUserConfig = {
  solidity: "0.8.4",
  networks,
};

export default config;
