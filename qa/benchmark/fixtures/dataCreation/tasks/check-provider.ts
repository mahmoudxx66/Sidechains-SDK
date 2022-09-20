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
    const signer = (await hre.ethers.getSigners())[0];
    console.log("Address:", (await signer.getAddress()).toString());
    console.log("Balance:", (await signer.getBalance()).toString());
    console.log(
      "Sending one token to random wallet to ensure that nonce != 0 for bug..."
    );
    const tx = await signer.sendTransaction({
      value: 1,
      to: "0xB791896a7C0685122AdCB77A350A6C73cefbDfdA",
    });
    await tx.wait();
    console.log("Done!");
  }
);
