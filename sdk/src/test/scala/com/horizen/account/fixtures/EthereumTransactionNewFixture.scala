package com.horizen.account.fixtures

import com.horizen.account.proof.SignatureSecp256k1
import com.horizen.account.state.GasUtil
import com.horizen.account.transaction.EthereumTransactionNew
import com.horizen.utils.BytesUtils
import org.web3j.crypto.Sign.SignatureData
import org.web3j.crypto.TransactionEncoder.createEip155SignatureData
import org.web3j.crypto._

import java.lang
import java.math.BigInteger
import java.util.Optional

trait EthereumTransactionNewFixture {


  def createLegacyTransaction(value: BigInteger,
                              nonce: BigInteger = BigInteger.ZERO,
                              pairOpt: Option[ECKeyPair] = None,
                              gasPrice: BigInteger = BigInteger.valueOf(10000),
                              gasLimit: BigInteger = GasUtil.TxGas): EthereumTransactionNew = {
    val unsignedTx = new EthereumTransactionNew(
      "0x1234567890123456789012345678901234567890", nonce, gasPrice, gasLimit, value, "", null)
    createSignedTransaction(unsignedTx, pairOpt)
  }


  def createLegacyEip155Transaction(value: BigInteger,
                              nonce: BigInteger = BigInteger.ZERO,
                              pairOpt: Option[ECKeyPair] = None,
                              gasPrice: BigInteger = BigInteger.valueOf(10000),
                              gasLimit: BigInteger = GasUtil.TxGas): EthereumTransactionNew = {
    val signData = new Sign.SignatureData(
      BytesUtils.fromHexString("26"), // chain id 1 (1*2+36)
      SignatureSecp256k1.EIP155_PARTIAL_SIGNATURE_RS,
      SignatureSecp256k1.EIP155_PARTIAL_SIGNATURE_RS
    )
    val unsignedTx = new EthereumTransactionNew(
      "0x1234567890123456789012345678901234567890", nonce, gasPrice, gasLimit, value, "", signData)
    createSignedLegacyEip155Transaction(unsignedTx, pairOpt, 1)
  }

  def createEIP1559Transaction(value: BigInteger,
                               nonce: BigInteger = BigInteger.ZERO,
                               pairOpt: Option[ECKeyPair] = None,
                               gasFee: BigInteger = BigInteger.valueOf(10000),
                               priorityGasFee: BigInteger = BigInteger.valueOf(10000),
                               gasLimit: BigInteger = GasUtil.TxGas): EthereumTransactionNew = {


    val unsignedTx = new EthereumTransactionNew(1997, "0x1234567890123456789012345678901234567890", nonce, gasLimit, priorityGasFee, gasFee, value, "", null)
    createSignedTransaction(unsignedTx, pairOpt)
  }

  private def createSignedTransaction(unsignedTx: EthereumTransactionNew, pairOpt: Option[ECKeyPair]): EthereumTransactionNew = {
    val message = unsignedTx.messageToSign()

    // Create a key pair, create tx signature and create ethereum Transaction
    val pair = pairOpt.getOrElse(Keys.createEcKeyPair)
    val msgSignature = Sign.signMessage(message, pair, true)
    new EthereumTransactionNew(unsignedTx,
      new SignatureData(msgSignature.getV, msgSignature.getR, msgSignature.getS)
    )
  }

  private def createSignedLegacyEip155Transaction(unsignedTx: EthereumTransactionNew, pairOpt: Option[ECKeyPair], chainId: Long): EthereumTransactionNew = {
    val message = unsignedTx.messageToSign()

    // Create a key pair, create tx signature and create ethereum Transaction
    val pair = pairOpt.getOrElse(Keys.createEcKeyPair)
    val msgSignature = Sign.signMessage(message, pair, true)
    new EthereumTransactionNew(unsignedTx,
      //new SignatureData(msgSignature.getV, msgSignature.getR, msgSignature.getS)
      createEip155SignatureData(new SignatureData(msgSignature.getV, msgSignature.getR, msgSignature.getS), chainId)

    )
  }

  def getEoa2EoaLegacyTransaction: EthereumTransactionNew = {
    new EthereumTransactionNew(
      "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
      BigInteger.valueOf(70L), // nonce
      BigInteger.valueOf(3).pow(9), // gasPrice
      GasUtil.TxGas,  // gasLimit
      BigInteger.TEN.pow(19), // value
      "",
      new Sign.SignatureData(
        BytesUtils.fromHexString("1c"),
        BytesUtils.fromHexString("2a4afbdd7e8d99c3df9dfd9e4ecd0afe018d8dec0b8b5fe1a44d5f30e7d0a5c5"),
        BytesUtils.fromHexString("7ca554a8317ff86eb6b23d06fa210d23e551bed58f58f803a87e5950aa47a9e9"))
    )
  }

  def getUnsignedEoa2EoaLegacyTransaction: EthereumTransactionNew = {
    new EthereumTransactionNew(
      "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
      BigInteger.valueOf(70L), // nonce
      BigInteger.valueOf(3).pow(9), // gasPrice
      GasUtil.TxGas,  // gasLimit
      BigInteger.TEN.pow(19), // value
      "",
      null
    )
  }

  def getContractCallEip155LegacyTransaction: EthereumTransactionNew = {
    new EthereumTransactionNew(
      "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
      BigInteger.valueOf(0x15), // nonce
      new BigInteger(BytesUtils.fromHexString("0a02ffee00")), // gasPrice
      new BigInteger(BytesUtils.fromHexString("0000008877")),  // gasLimit
      BigInteger.ZERO, // value
      "0xa9059cbb0000000000000000000000000ee513f5366075aa80565f2b0cc2dc0adc76a475000000000000000000000000000000000000000000000000b917d212e0933000",
      new Sign.SignatureData(
        BytesUtils.fromHexString("25"),
        BytesUtils.fromHexString("b83e51baa9bb20d7b281032f44c1ed75f25f69ac47ac26d97e66859ea80c1295"),
        BytesUtils.fromHexString("22d794fa14567f77d7a6e6e9f780094010d70991c3028b4ea50faf62f1631e52"))
    )
  }

  def getContractDeploymentEip1559Transaction: EthereumTransactionNew = {
    new EthereumTransactionNew(
      31337,
      "",
      BigInteger.valueOf(33L), // nonce
      new BigInteger(BytesUtils.fromHexString("00e469c3")),  // gasLimit
      new BigInteger(BytesUtils.fromHexString("3b9aca00")),  // maxPriorityFeePerGas
      new BigInteger(BytesUtils.fromHexString("0c6e64e7f7")),  // maxFeePerGas
      BigInteger.ZERO, // value
      "c7265637420783d223136302220793d22323335222077696474683d2231383022206865696768743d22353022207374796c653d226",
      new Sign.SignatureData(
        BytesUtils.fromHexString("1c"),
        BytesUtils.fromHexString("1f9202e5b7e4d65e381c63b9a6235838e7bc5d6a0ee0f070be40fd3913855d3c"),
        BytesUtils.fromHexString("1f9202e5b7e4d65e381c63b9a6235838e7bc5d6a0ee0f070be40fd3913855d3c"))
    )
  }

  def getEoa2EoaEip1559Transaction: EthereumTransactionNew = {
    new EthereumTransactionNew(
      31337,
      "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
      BigInteger.valueOf(33L), // nonce
      GasUtil.TxGas,  // gasLimit
      new BigInteger(BytesUtils.fromHexString("09172ea8")),  // maxPriorityFeePerGas
      new BigInteger(BytesUtils.fromHexString("0390a21a17")),  // maxFeePerGas
      new BigInteger(BytesUtils.fromHexString("320ec7c4e2a000")), // value
      "",
      new Sign.SignatureData(
        BytesUtils.fromHexString("1c"),
        BytesUtils.fromHexString("e430ed7a69eb23bd96d543d6acb605af31d3eb6b1967ca4a69f31245527d942e"),
        BytesUtils.fromHexString("06d2c6034ad1905b4121aa036a382795d8f9fb2f801eafb256febb09f88f6d46"))
    )
  }

  def getUnsignedEoa2EoaEip1559Transaction: EthereumTransactionNew = {
    new EthereumTransactionNew(
      31337,
      "0x70997970C51812dc3A010C7d01b50e0d17dc79C8",
      BigInteger.valueOf(33L), // nonce
      GasUtil.TxGas,  // gasLimit
      new BigInteger(BytesUtils.fromHexString("09172ea8")),  // maxPriorityFeePerGas
      new BigInteger(BytesUtils.fromHexString("0390a21a17")),  // maxFeePerGas
      new BigInteger(BytesUtils.fromHexString("320ec7c4e2a000")), // value
      "",
      null
    )
  }

  def getEoa2EoaEip155LegacyTransaction: EthereumTransactionNew = {
    new EthereumTransactionNew(
      "0x3535353535353535353535353535353535353535",
      BigInteger.valueOf(9L), // nonce
      new BigInteger(BytesUtils.fromHexString("0a02ffee00")), // gasPrice
      GasUtil.TxGas,  // gasLimit
      BigInteger.TEN.pow(18), // value
      "",
      new Sign.SignatureData(
        BytesUtils.fromHexString("25"), // chain id 1 (1*2+35)
        BytesUtils.fromHexString("28EF61340BD939BC2195FE537567866003E1A15D3C71FF63E1590620AA636276"),
        BytesUtils.fromHexString("67CBE9D8997F761AECB703304B3800CCF555C9F3DC64214B297FB1966A3B6D83")
      )
    )
  }

  def getPartiallySignedEip155LegacyTransaction: EthereumTransactionNew = {
    new EthereumTransactionNew(
      "0x3535353535353535353535353535353535353535",
      BigInteger.valueOf(9L), // nonce
      new BigInteger(BytesUtils.fromHexString("0a02ffee00")), // gasPrice
      GasUtil.TxGas,  // gasLimit
      BigInteger.TEN.pow(18), // value
      "",
      new SignatureData(
        BytesUtils.fromHexString("25"), // chain id 1 (1*2+35)
        SignatureSecp256k1.EIP155_PARTIAL_SIGNATURE_RS,
        SignatureSecp256k1.EIP155_PARTIAL_SIGNATURE_RS
      )
    )
  }

  def copyEip1599EthereumTransaction(
                                      inTx: EthereumTransactionNew,
                                      inChainId: lang.Long = null,
                                      inTo: Optional[String] = null,
                                      inNonce: BigInteger = null,
                                      inGasLimit: BigInteger = null,
                                      inMaxPriorityFeePerGas: BigInteger = null,
                                      inMaxFeePerGas: BigInteger = null,
                                      inValue: BigInteger = null,
                                      inData: String = null,
                                      inSignatureData: Optional[SignatureData] = null): EthereumTransactionNew =
  {
    val chainId : Long = if (inChainId != null) inChainId   else inTx.getChainId

    val to : String = if (inTo != null) {
      inTo.get()
    } else {
        if (inTx.getTo != null) {
          BytesUtils.toHexString(inTx.getTo.address())
        }
        else
          null
    }

    val nonce    = if (inNonce != null) inNonce       else inTx.getNonce
    val gasLimit = if (inGasLimit != null) inGasLimit else inTx.getGasLimit
    val maxPriorityFeePerGas = if (inMaxPriorityFeePerGas != null) inMaxPriorityFeePerGas else inTx.getMaxPriorityFeePerGas
    val maxFeePerGas = if (inMaxFeePerGas != null) inMaxFeePerGas else inTx.getMaxFeePerGas
    val value = if (inValue != null) inValue else inTx.getValue
    val data = if (inData != null) inData else BytesUtils.toHexString(inTx.getData)

    val signatureData = if (inSignatureData != null) inSignatureData.get() else inTx.getSignatureData

    new EthereumTransactionNew(chainId, to, nonce, gasLimit, maxPriorityFeePerGas, maxFeePerGas, value, data, signatureData)

  }

  def copyLegacyEthereumTransaction(
                                      inTx: EthereumTransactionNew,
                                      inTo: Optional[String] = null,
                                      inNonce: BigInteger = null,
                                      inGasLimit: BigInteger = null,
                                      inGasPrice: BigInteger = null,
                                      inValue: BigInteger = null,
                                      inData: String = null,
                                      inSignatureData: Optional[SignatureData] = null): EthereumTransactionNew =
  {
    val to : String = if (inTo != null) {
      inTo.get()
    } else {
      if (inTx.getTo != null) {
        BytesUtils.toHexString(inTx.getTo.address())
      }
      else
        null
    }

    val nonce    = if (inNonce != null) inNonce       else inTx.getNonce
    val gasLimit = if (inGasLimit != null) inGasLimit else inTx.getGasLimit
    val gasPrice = if (inGasPrice != null) inGasPrice else inTx.getGasPrice
    val value = if (inValue != null) inValue else inTx.getValue
    val data = if (inData != null) inData else BytesUtils.toHexString(inTx.getData)

    val signatureData = if (inSignatureData != null) inSignatureData.get() else inTx.getSignatureData

    new EthereumTransactionNew(to, nonce, gasPrice, gasLimit, value, data, signatureData)

  }

}

