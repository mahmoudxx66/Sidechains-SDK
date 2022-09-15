import { task } from "hardhat/config";
import "@nomiclabs/hardhat-waffle";
import fs from "fs/promises";
import { WalletInfo } from "./helpers/types";

interface GenWalletArgs {
  amount: number;
  outFile?: string;
}

task(
  "gen-wallets",
  "Generates private keys and addresses for n wallets",
  async ({ amount, outFile }: GenWalletArgs, hre) => {
    const wallets: WalletInfo[] = [];
    for (let i = 0; i < amount; i++) {
      const w = hre.ethers.Wallet.createRandom();
      wallets.push({ address: w.address, privateKey: w.privateKey });
    }
    await fs.writeFile(outFile, JSON.stringify(wallets, undefined, 2));
  }
)
  .addParam<number>("amount", "The amount of wallets")
  .addParam<string>("outFile", "The name of the output file");
