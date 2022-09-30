import { task } from "hardhat/config";
import "@nomiclabs/hardhat-waffle";
import { writeFile } from "fs/promises";
import { AllowedTokenTypes, TokenInfo, TokenType } from "./helpers/types";
import { generate_blocks } from "./helpers/generate-block";
interface DeploymentArguments {
  type: string;
  name: string;
  symbol: string;
  outFile: string;
}

task(
  "deploy",
  "Deploys a test token",
  async (args: DeploymentArguments, hre) => {
    if (!AllowedTokenTypes.find((x) => x === args.type)) {
      console.error(
        "Token type invalid. Must be in <" + AllowedTokenTypes.join("|") + ">"
      );
      throw "Execution failed";
    }

    const signers = await hre.ethers.getSigners();
    console.log("Signer:", signers.map((x) => x.address)[0]);

    await hre.run("clean");
    await hre.run("compile");

    const contractFileNamePrefix = `Test${args.type.toUpperCase()}`;
    const TestToken = await hre.ethers.getContractFactory(
      contractFileNamePrefix,
      signers[0]
    );

    console.info("Deploying", contractFileNamePrefix);
    const testToken = await TestToken.deploy(args.name, args.symbol);
    generate_blocks(1);
    await testToken.deployed();

    console.info(`${contractFileNamePrefix} deployed at:`);
    console.info(testToken.address);
    const tokenInfo: TokenInfo = {
      address: testToken.address,
      name: args.name,
      symbol: args.symbol,
      deployer: signers[0].address,
      type: args.type as TokenType,
    };
    await writeFile(args.outFile, JSON.stringify(tokenInfo));

    console.info("Deployment output written to", args.outFile);
  }
)
  .addParam("name", "The name of the token")
  .addParam("symbol", "The symbol of the token")
  .addParam("type", "The token type <erc20|erc721|erc1155>")
  .addParam("outFile", "The file to write the address into");
