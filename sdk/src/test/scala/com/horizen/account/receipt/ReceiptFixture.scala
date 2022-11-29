package com.horizen.account.receipt


import com.horizen.evm.interop.EvmLog
import com.horizen.evm.utils.{Address, Hash}
import com.horizen.utils.BytesUtils
import scorex.crypto.hash.Keccak256

import java.math.BigInteger
import scala.collection.mutable.ListBuffer
import scala.util.Random


trait ReceiptFixture {
    def getRandomHash(): Array[Byte] = {
      val hashBuffer = new Array[Byte](Hash.LENGTH)
      Random.nextBytes(hashBuffer)

      hashBuffer
    }

    def createTestEvmLog(addressBytes: Option[Array[Byte]]): EvmLog = {
      // random address and fixed topics/data
      val addressBytesTemp: Array[Byte] = addressBytes.getOrElse(getRandomHash().slice(0, 20))
      val address = Address.fromBytes(addressBytesTemp)

      val topics = new Array[Hash](4)
      topics(0) = Hash.fromBytes(BytesUtils.fromHexString("0000000000000000000000000000000000000000000000000000000000000000"))
      topics(1) = Hash.fromBytes(BytesUtils.fromHexString("1111111111111111111111111111111111111111111111111111111111111111"))
      topics(2) = Hash.fromBytes(BytesUtils.fromHexString("2222222222222222222222222222222222222222222222222222222222222222"))
      topics(3) = Hash.fromBytes(BytesUtils.fromHexString("3333333333333333333333333333333333333333333333333333333333333333"))

      val data = BytesUtils.fromHexString("aabbccddeeff")
      new EvmLog(address, topics, data)
    }

  def createTestEthereumReceipt(txType: Integer, num_logs: Integer = 2, contractAddressPresence : Boolean = true, txHash: Option[Array[Byte]], address: Array[Byte] = Address.addressZero().toBytes): EthereumReceipt = {
    val txHashTemp: Array[Byte] = txHash.getOrElse(getRandomHash())

    val logs = new ListBuffer[EvmLog]
    for (_ <- 1 to num_logs)
      logs += createTestEvmLog(Some(address))

    val contractAddress = if (contractAddressPresence) {
      BytesUtils.fromHexString("1122334455667788990011223344556677889900")
    } else {
      new Array[Byte](0)
    }
    val consensusDataReceipt = new EthereumConsensusDataReceipt(txType, 1, BigInteger.valueOf(1000), logs)
    val receipt = EthereumReceipt(consensusDataReceipt,
      txHashTemp, 33, Keccak256.hash("blockhash".getBytes).asInstanceOf[Array[Byte]], 22,
      BigInteger.valueOf(1234567),
      contractAddress
      )
    receipt
  }

  def createTestEthereumConsensusDataReceipt(txType: Integer, num_logs: Integer, address: Array[Byte] = null): EthereumConsensusDataReceipt = {
    val txHash = new Array[Byte](32)
    Random.nextBytes(txHash)
    val logs = new ListBuffer[EvmLog]
    for (_ <- 1 to num_logs)
      logs += createTestEvmLog(Some(address))
    new EthereumConsensusDataReceipt(txType, 1, BigInteger.valueOf(1000), logs)
  }

}
