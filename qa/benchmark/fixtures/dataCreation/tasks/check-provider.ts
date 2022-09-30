import { task } from "hardhat/config";
import "@nomiclabs/hardhat-waffle";
import fs from "fs/promises";
import { WalletInfo } from "./helpers/types";
import { generate_blocks } from "./helpers/generate-block";

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
      "Sending one token to a wallet to ensure that nonce != 0 for bug..."
    );
    const txp = signer.sendTransaction({
      value: 1,
      to: "0xB791896a7C0685122AdCB77A350A6C73cefbDfdA",
    });
    console.log("Generating block");
    generate_blocks(1);
    console.log("Waiting for tx to finish...")
    await (await txp).wait();
    console.log("Done!");
  }
);
