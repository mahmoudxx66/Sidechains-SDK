export interface WalletInfo {
  privateKey: string;
  address: string;
}
export interface TokenInfo {
  address: string;
  deployer: string;
  type: TokenType;
  name: string;
  symbol: string;
}

export interface TxInfo {
  hash: string;
}

export type TokenType = "erc20" | "erc1155" | "erc721";
export const AllowedTokenTypes: readonly TokenType[] = <const>[
  "erc20",
  "erc1155",
  "erc721",
];
