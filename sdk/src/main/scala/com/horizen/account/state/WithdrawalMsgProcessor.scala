package com.horizen.account.state

import com.google.common.primitives.{Bytes, Ints}
import com.horizen.account.abi.ABIDecoder.{OP_CODE_LENGTH, getOpCodeFromData}
import com.horizen.account.abi.{ABIDecoder, ABIEncodable, ABIListEncoder}
import com.horizen.account.proposition.AddressProposition
import com.horizen.account.state.AbstractFakeSmartContractMsgProcessor.getABIMethodId
import com.horizen.account.utils.ZenWeiConverter
import com.horizen.proposition.MCPublicKeyHashProposition
import com.horizen.utils.{BytesUtils, ZenCoinsUtils}
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.{StaticStruct, Type}
import org.web3j.abi.datatypes.generated.{Bytes20, Bytes32, Uint32}
import scorex.crypto.hash.Keccak256

import java.util

trait WithdrawalRequestProvider {
  private[horizen] def getListOfWithdrawalReqRecords(epochNum: Int, view: BaseAccountStateView): Seq[WithdrawalRequest]
}

object WithdrawalMsgProcessor extends AbstractFakeSmartContractMsgProcessor with WithdrawalRequestProvider {

  override val fakeSmartContractAddress: AddressProposition = new AddressProposition(BytesUtils.fromHexString("0000000000000000000011111111111111111111"))

  override val fakeSmartContractCodeHash: Array[Byte] =
    Keccak256.hash("WithdrawalRequestSmartContractCodeHash")

  val GetListOfWithdrawalReqsCmdSig: String = getABIMethodId("getWithdrawalRequests(uint32)")
  val AddNewWithdrawalReqCmdSig: String = getABIMethodId("submitWithdrawalRequests(bytes20)")

  //TODO Define a proper amount of gas spent for each operation
  val GasSpentForGetListOfWithdrawalReqsCmd: java.math.BigInteger = java.math.BigInteger.ONE
  val GasSpentForGetListOfWithdrawalReqsFailure: java.math.BigInteger = java.math.BigInteger.ONE
  val GasSpentForAddNewWithdrawalReqCmd: java.math.BigInteger = java.math.BigInteger.ONE
  val GasSpentForAddNewWithdrawalReqFailure: java.math.BigInteger = java.math.BigInteger.ONE
  val GasSpentForGenericFailure: java.math.BigInteger = java.math.BigInteger.ONE

  val MaxWithdrawalReqsNumPerEpoch = 3999
  val DustThresholdInWei: java.math.BigInteger = ZenWeiConverter.convertZenniesToWei(ZenCoinsUtils.getMinDustThreshold(ZenCoinsUtils.MC_DEFAULT_FEE_RATE))


  override def process(msg: Message, view: BaseAccountStateView): ExecutionResult = {
    //TODO: check errors in Ethereum, maybe for some kind of errors there a predefined types or codes

    try {
      if (!canProcess(msg, view)) {
        log.error(s"Cannot process message $msg")
        new InvalidMessage(new IllegalArgumentException(s"Cannot process message $msg"))
      }
      else {

        val functionSig = BytesUtils.toHexString(getOpCodeFromData(msg.getData))
        functionSig match {
          case GetListOfWithdrawalReqsCmdSig => execGetListOfWithdrawalReqRecords(msg, view)
          case AddNewWithdrawalReqCmdSig => execAddWithdrawalRequest(msg, view)
          case _ => log.debug(s"Requested function does not exist. Function signature: $functionSig")
            new ExecutionFailed(GasSpentForGenericFailure, new IllegalArgumentException(s"Requested function does not exist"))
        }
      }
    }
    catch {
      case e: Exception =>
        log.error(s"Exception while processing message: $msg", e)
        new ExecutionFailed(GasSpentForGenericFailure, e)
    }

  }

  private def getWithdrawalEpochCounter(view: BaseAccountStateView, epochNum: Int) = {
    val key = getWithdrawalEpochCounterKey(epochNum)
    val wrCounterInBytesPadded = view.getAccountStorage(fakeSmartContractAddress.address(), key).get
    val wrCounterInBytes = wrCounterInBytesPadded.drop(wrCounterInBytesPadded.length - Ints.BYTES)
    val numOfWithdrawalReqs = Ints.fromByteArray(wrCounterInBytes)

    numOfWithdrawalReqs
  }

  private[horizen] def setWithdrawalEpochCounter(view: BaseAccountStateView, currentEpochNum: Int, nextNumOfWithdrawalReqs: Int): Unit = {
    val nextNumOfWithdrawalReqsBytes = Ints.toByteArray(nextNumOfWithdrawalReqs)
    val paddedNextNumOfWithdrawalReqs = Bytes.concat(new Array[Byte](32 - nextNumOfWithdrawalReqsBytes.length), nextNumOfWithdrawalReqsBytes)
    val wrCounterKey = getWithdrawalEpochCounterKey(currentEpochNum)
    view.updateAccountStorage(fakeSmartContractAddress.address(), wrCounterKey, paddedNextNumOfWithdrawalReqs).get
  }


  override private[horizen] def getListOfWithdrawalReqRecords(epochNum: Int, view: BaseAccountStateView): Seq[WithdrawalRequest] = {
    val numOfWithdrawalReqs: Int = getWithdrawalEpochCounter(view, epochNum)

    val listOfWithdrawalReqs = (1 to numOfWithdrawalReqs).map(index => {
      val currentKey = getWithdrawalRequestsKey(epochNum, index)
      WithdrawalRequestSerializer.parseBytes(view.getAccountStorageBytes(fakeSmartContractAddress.address(), currentKey).get)
    })
    listOfWithdrawalReqs
  }

  protected def execGetListOfWithdrawalReqRecords(msg: Message, view: BaseAccountStateView): ExecutionResult = {
    try {
      require(msg.getData.length == OP_CODE_LENGTH + GetListOfWithdrawalRequestsCmdInputDecoder.ABIDataParamsLengthInBytes,
        s"Wrong data length ${msg.getData.length}") //TODO should any length between OP_CODE_LENGTH to OP_CODE_LENGTH + 32 be supported?

      val inputParams = GetListOfWithdrawalRequestsCmdInputDecoder.decode(msg.getData).get
      val epochNum = inputParams.epochNum
      val listOfWithdrawalReqs = getListOfWithdrawalReqRecords(epochNum, view)

      val abiEncodedList = WithdrawalRequestsListEncoder.encode(listOfWithdrawalReqs)
      //Evm log
      new ExecutionSucceeded(GasSpentForGetListOfWithdrawalReqsCmd, abiEncodedList)
    }
    catch {
      case e: Exception =>
        log.debug(s"Error while getting Withdrawal Request list: ${e.getMessage}", e)
        new ExecutionFailed(GasSpentForGetListOfWithdrawalReqsFailure, e)
    }

  }


  private[horizen] def checkWithdrawalRequestValidity(msg: Message, view: BaseAccountStateView): Unit = {
    val withdrawalAmount = msg.getValue

    if (msg.getData.length != OP_CODE_LENGTH + AddWithdrawalRequestCmdInputDecoder.ABIDataParamsLengthInBytes) {
      log.error(s"Wrong message data field length: ${msg.getData.length}")
      throw new IllegalArgumentException("Wrong message data field length")
    }
    else if (!ZenWeiConverter.isValidZenAmount(withdrawalAmount)) {
      log.error(s"Withdrawal amount is not a valid Zen amount: $withdrawalAmount")
      throw new IllegalArgumentException("Withdrawal amount is not a valid Zen amount")
    }
    else if (withdrawalAmount.compareTo(DustThresholdInWei) < 0) {
      log.error(s"Withdrawal amount is under the dust threshold: $withdrawalAmount")
      throw new IllegalArgumentException("Withdrawal amount is under the dust threshold")
    }
    else {
      val balance = view.getBalance(msg.getFrom.address()).get
      if (balance.compareTo(withdrawalAmount) < 0) {
        log.error(s"Insufficient balance amount: balance: $balance, requested withdrawal amount: $withdrawalAmount")
        throw new IllegalArgumentException("Insufficient balance amount")
      }
    }

  }

  protected def execAddWithdrawalRequest(msg: Message, view: BaseAccountStateView): ExecutionResult = {
    try {
      checkWithdrawalRequestValidity(msg, view)
      val currentEpochNum = view.getWithdrawalEpochInfo.epoch
      val numOfWithdrawalReqs = getWithdrawalEpochCounter(view, currentEpochNum)
      if (numOfWithdrawalReqs >= MaxWithdrawalReqsNumPerEpoch) {
        log.debug(s"Reached maximum number of Withdrawal Requests per epoch: request is invalid")
        return new ExecutionFailed(GasSpentForAddNewWithdrawalReqFailure, new IllegalArgumentException("Reached maximum number of Withdrawal Requests per epoch"))
      }

      val nextNumOfWithdrawalReqs: Int = numOfWithdrawalReqs + 1
      setWithdrawalEpochCounter(view, currentEpochNum, nextNumOfWithdrawalReqs)

      val inputParams = AddWithdrawalRequestCmdInputDecoder.decode(msg.getData).get
      val withdrawalAmount = msg.getValue
      val request = WithdrawalRequest(inputParams.mcAddr, withdrawalAmount)
      val requestInBytes = request.bytes
      view.updateAccountStorageBytes(fakeSmartContractAddress.address(), getWithdrawalRequestsKey(currentEpochNum, nextNumOfWithdrawalReqs), requestInBytes).get

      view.subBalance(msg.getFrom.address(), withdrawalAmount).get
      val abiEncodedResult = request.encode
      new ExecutionSucceeded(GasSpentForAddNewWithdrawalReqCmd, abiEncodedResult)
    }
    catch {
      case e: Exception =>
        log.debug("Exception while adding a new Withdrawal Request", e)
        new ExecutionFailed(GasSpentForAddNewWithdrawalReqFailure, e)
    }

  }

  private[horizen] def calculateKey(keySeed: Array[Byte]): Array[Byte] = {
    Keccak256.hash(keySeed)
  }

  private[horizen] def getWithdrawalEpochCounterKey(withdrawalEpoch: Int): Array[Byte] = {
    calculateKey(Bytes.concat("withdrawalEpochCounter".getBytes, Ints.toByteArray(withdrawalEpoch)))
  }

  private[horizen] def getWithdrawalRequestsKey(withdrawalEpoch: Int, counter: Int): Array[Byte] = {
    calculateKey(Bytes.concat("withdrawalRequests".getBytes, Ints.toByteArray(withdrawalEpoch), Ints.toByteArray(counter)))
  }


}

object AddWithdrawalRequestCmdInputDecoder extends ABIDecoder[AddWithdrawalRequestCmdInput] {

  override val ListOfABIParamTypes = org.web3j.abi.Utils.convert(util.Arrays.asList(new TypeReference[Bytes20]() {}))

  override def createType(listOfParams: util.List[Type[_]]): AddWithdrawalRequestCmdInput = {
    AddWithdrawalRequestCmdInput(new MCPublicKeyHashProposition(listOfParams.get(0).asInstanceOf[Bytes20].getValue))
  }

}

case class AddWithdrawalRequestCmdInput(mcAddr: MCPublicKeyHashProposition) extends ABIEncodable {
  override type M = AddWithdrawalRequestCmdInput

  override def asABIType(): StaticStruct = {
    new StaticStruct(
      new Bytes20(mcAddr.bytes())
    )
  }
}

object GetListOfWithdrawalRequestsCmdInputDecoder extends ABIDecoder[GetListOfWithdrawalRequestsInputCmd] {
  override val ListOfABIParamTypes = org.web3j.abi.Utils.convert(util.Arrays.asList(new TypeReference[Uint32]() {}))

  override def createType(listOfParams: util.List[Type[_]]): GetListOfWithdrawalRequestsInputCmd = {
    GetListOfWithdrawalRequestsInputCmd(listOfParams.get(0).asInstanceOf[Uint32].getValue.intValue())
  }

}

case class GetListOfWithdrawalRequestsInputCmd(epochNum: Int) extends ABIEncodable {
  override type M = GetListOfWithdrawalRequestsInputCmd

  override def asABIType(): StaticStruct = {
   new StaticStruct(
      new Uint32(epochNum)
    )
  }
}

object WithdrawalRequestsListEncoder extends ABIListEncoder[WithdrawalRequest]