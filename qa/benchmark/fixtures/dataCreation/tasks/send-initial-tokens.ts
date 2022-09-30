import { task } from "hardhat/config";
import "@nomiclabs/hardhat-waffle";
import { readFile, writeFile } from "fs/promises";
import { TokenInfo, TxInfo, WalletInfo } from "./helpers/types";
import { HardhatRuntimeEnvironment } from "hardhat/types";
import { generate_blocks } from "./helpers/generate-block";

interface TaskArguments {
  tokenFile: string;
  walletFile: string;
  amount: string;
  outFile: string;
}

task(
  "send-initial-tokens",
  "Sends the initial test tokens to the given list of accounts [deprecated]",
  async (args: TaskArguments, hre) => {
    const tokenInfo: TokenInfo = JSON.parse(
      await readFile(args.tokenFile, { encoding: "utf-8" })
    );
    const wallets: WalletInfo[] = JSON.parse(
      await readFile(args.walletFile, { encoding: "utf-8" })
    );
    let txInfos: TxInfo[];
    switch (tokenInfo.type) {
      case "erc20":
        txInfos = await sendERC20(hre, wallets, args.amount, tokenInfo.address);
        break;
      case "erc1155":
        txInfos = await sendERC1155(
          hre,
          wallets,
          args.amount,
          tokenInfo.address
        );
        break;
      case "erc721":
        txInfos = await sendERC721(
          hre,
          wallets,
          args.amount,
          tokenInfo.address
        );
        break;
    }

    await writeFile(args.outFile, JSON.stringify(txInfos));
  }
)
  .addParam("tokenFile", "The file containing the token information")
  .addParam("walletFile", "The file containing the list of wallets")
  .addParam("amount", "The number of tokens to send to each wallet")
  .addParam("outFile", "The file which will contain the transactions");

const sendERC20 = async (
  hre: HardhatRuntimeEnvironment,
  wallets: WalletInfo[],
  amount: string,
  address: string
) => {
  console.info("Sending initial ERC20 tokens");
  const TestToken = await (
    await hre.ethers.getContractFactory("TestERC20")
  ).attach(address);

  const txs: TxInfo[] = [];

  const chunkSize = 250;
  for (let i = 0; i < wallets.length; i += chunkSize) {
    const chunk = wallets.slice(i, i + chunkSize).map((x) => x.address);
    console.log(
      `Sending tokens to wallets ${i + 1} to ${i + chunkSize} of ${
        wallets.length
      }`
    );
    const tx = await TestToken.batchTransfer(chunk, amount);
    txs.push({ hash: tx.hash });
    generate_blocks(1);
    await tx.wait();
  }
  return txs;
};
const sendERC721 = async (
  hre: HardhatRuntimeEnvironment,
  wallets: WalletInfo[],
  amount: string,
  address: string
) => {
  throw "Not implemented";
  const txs: TxInfo[] = [];
  return txs;
};
const sendERC1155 = async (
  hre: HardhatRuntimeEnvironment,
  wallets: WalletInfo[],
  amount: string,
  address: string
) => {
  throw "Not implemented";
  const txs: TxInfo[] = [];
  return txs;
};
