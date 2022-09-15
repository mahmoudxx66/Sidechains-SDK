import { task } from "hardhat/config";
import "@nomiclabs/hardhat-waffle";
import fs from "fs/promises";
import { WalletInfo } from "./helpers/types";

task(
  "check-provider",
  "Just calls small functions on the provider to check functionality",
  async (args, hre) => {
    const network = await hre.ethers.provider.getNetwork();
    console.log(`Name: ${network.name}`);
    console.log(`ChainID: ${network.chainId}`);
    console.log(`GasPrice: ${await hre.ethers.provider.getGasPrice()}`);
    console.log(`URL: ${await hre.ethers.provider.connection.url}`);
  }
);
