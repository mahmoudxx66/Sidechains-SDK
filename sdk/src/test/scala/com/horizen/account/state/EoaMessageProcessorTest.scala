package com.horizen.account.state

import com.horizen.account.proposition.AddressProposition
import com.horizen.evm.StateDB
import com.horizen.fixtures.SecretFixture
import org.junit.Assert.{assertArrayEquals, assertEquals, assertFalse, assertTrue}
import org.junit.Test
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatestplus.junit.JUnitSuite
import org.scalatestplus.mockito.MockitoSugar

import java.math.BigInteger
import scala.util.{Failure, Success}

class EoaMessageProcessorTest extends JUnitSuite
  with MockitoSugar
  with SecretFixture
  with MessageProcessorFixture {


  @Test
  def init(): Unit = {
    val mockStateView: AccountStateView = mock[AccountStateView]

    try {
      EoaMessageProcessor.init(mockStateView)
    } catch {
      case ex: Exception =>
        fail("Initialization failed", ex)
    }
  }

  @Test
  def canProcess(): Unit = {
    val address: AddressProposition = getAddressProposition(12345L)
    val value: java.math.BigInteger = java.math.BigInteger.ONE
    val emptyData: Array[Byte] = Array.emptyByteArray
    val msg: Message = getMessage(address, value, emptyData)

    val mockStateView: AccountStateView = mock[AccountStateView]


    // Test 1: send to EOA account, tx with empty "data"
    Mockito.when(mockStateView.isEoaAccount(ArgumentMatchers.any[Array[Byte]])).thenAnswer(args => {
      val addressBytes: Array[Byte] = args.getArgument(0)
      assertArrayEquals("Different address found", msg.getTo.address(), addressBytes)
      true
    })

    assertTrue("Message for EoaMessageProcessor cannot be processed", EoaMessageProcessor.canProcess(msg, mockStateView))


    // Test 2: send to EOA account, tx with no-empty "data"
    val data: Array[Byte] = new Array[Byte](1000)
    val msgWithData: Message = getMessage(address, value, data)

    assertTrue("Message for EoaMessageProcessor cannot be processed", EoaMessageProcessor.canProcess(msgWithData, mockStateView))


    // Test 2: Failure: send to smart contract account
    Mockito.reset(mockStateView)
    Mockito.when(mockStateView.isEoaAccount(ArgumentMatchers.any[Array[Byte]])).thenAnswer(args => {
      val addressBytes: Array[Byte] = args.getArgument(0)
      assertArrayEquals("Different address found", msg.getTo.address(), addressBytes)
      false
    })

    assertFalse("Message for EoaMessageProcessor wrongly can be processed", EoaMessageProcessor.canProcess(msg, mockStateView))
  }

  @Test
  def process(): Unit = {
    val address: AddressProposition = getAddressProposition(12345L)
    val value: java.math.BigInteger = java.math.BigInteger.ONE
    val emptyData: Array[Byte] = Array.emptyByteArray
    val msg: Message = getMessage(address, value, emptyData)

    val mockStateView: AccountStateView = mock[AccountStateView]


    // Test 1: Success: no failures during balance changes
    Mockito.when(mockStateView.subBalance(ArgumentMatchers.any[Array[Byte]], ArgumentMatchers.any[BigInteger])).thenAnswer(args => {
      val addressBytes: Array[Byte] = args.getArgument(0)
      val amount: BigInteger = args.getArgument(1)

      assertArrayEquals("Different address found", msg.getFrom.address(), addressBytes)
      assertEquals("Different amount found", msg.getValue, amount)

      Success()
    })

    Mockito.when(mockStateView.addBalance(ArgumentMatchers.any[Array[Byte]], ArgumentMatchers.any[BigInteger])).thenAnswer(args => {
      val addressBytes: Array[Byte] = args.getArgument(0)
      val amount: BigInteger = args.getArgument(1)

      assertArrayEquals("Different address found", msg.getTo.address(), addressBytes)
      assertEquals("Different amount found", msg.getValue, amount)
      Success()
    })

    EoaMessageProcessor.process(msg, mockStateView) match {
      case es: ExecutionSucceeded =>
        assertEquals("Different gas found", EoaMessageProcessor.GAS_USED, es.gasUsed())
        assertArrayEquals("Different return data found", Array.emptyByteArray, es.returnData())
      case _: ExecutionFailed | _: InvalidMessage => fail("Execution failure received")
    }


    // Test 2: Failure during subBalance
    Mockito.reset(mockStateView)
    val exception = new Exception("some error")
    Mockito.when(mockStateView.subBalance(ArgumentMatchers.any[Array[Byte]], ArgumentMatchers.any[BigInteger])).thenAnswer(args => {
      Failure(exception)
    })

    EoaMessageProcessor.process(msg, mockStateView) match {
      case _: ExecutionSucceeded | _: InvalidMessage => fail("Execution failure expected")
      case ef: ExecutionFailed =>
        assertEquals("Different gas found", EoaMessageProcessor.GAS_USED, ef.gasUsed())
        assertEquals("Different exception found", exception, ef.getReason.getCause)
    }


    // Test 3: Failure during addBalance
    Mockito.reset(mockStateView)

    Mockito.when(mockStateView.subBalance(ArgumentMatchers.any[Array[Byte]], ArgumentMatchers.any[BigInteger])).thenAnswer(args => {
      val addressBytes: Array[Byte] = args.getArgument(0)
      val amount: BigInteger = args.getArgument(1)

      assertArrayEquals("Different address found", msg.getFrom.address(), addressBytes)
      assertEquals("Different amount found", msg.getValue, amount)

      Success()
    })

    Mockito.when(mockStateView.addBalance(ArgumentMatchers.any[Array[Byte]], ArgumentMatchers.any[BigInteger])).thenAnswer(args => {
      Failure(exception)
    })

    EoaMessageProcessor.process(msg, mockStateView) match {
      case _: ExecutionSucceeded | _: InvalidMessage => fail("Execution failure expected")
      case ef: ExecutionFailed =>
        assertEquals("Different gas found", EoaMessageProcessor.GAS_USED, ef.gasUsed())
        assertEquals("Different exception found", exception, ef.getReason.getCause)
    }
  }
}
