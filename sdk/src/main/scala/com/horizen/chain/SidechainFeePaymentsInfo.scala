package com.horizen.chain

import com.horizen.box.ZenBox
import scorex.core.serialization.ScorexSerializer
import scorex.util.serialization.{Reader, Writer}
import com.horizen.transaction.FeePaymentsTransaction
import com.horizen.transaction.FeePaymentsTransactionSerializer
import scala.collection.JavaConverters._


case class SidechainFeePaymentsInfo(transaction: FeePaymentsTransaction) extends AbstractFeePaymentsInfo {
  override type M = SidechainFeePaymentsInfo

  override def serializer: ScorexSerializer[M] = FeePaymentsInfoSerializer
}

object SidechainFeePaymentsInfo {
  def apply(feePayments: Seq[ZenBox]): SidechainFeePaymentsInfo = {
    SidechainFeePaymentsInfo(new FeePaymentsTransaction(feePayments.asJava, FeePaymentsTransaction.FEE_PAYMENTS_TRANSACTION_VERSION))
  }
}


object FeePaymentsInfoSerializer extends ScorexSerializer[SidechainFeePaymentsInfo] {
  override def serialize(feePaymentsInfo: SidechainFeePaymentsInfo, w: Writer): Unit = {
    FeePaymentsTransactionSerializer.getSerializer.serialize(feePaymentsInfo.transaction, w)
  }

  override def parse(r: Reader): SidechainFeePaymentsInfo = {
    val transaction = FeePaymentsTransactionSerializer.getSerializer.parse(r)

    SidechainFeePaymentsInfo(transaction)
  }
}