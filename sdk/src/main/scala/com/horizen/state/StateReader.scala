package com.horizen.state

import com.horizen.account.state.WithdrawalRequest
import com.horizen.block.WithdrawalEpochCertificate
import com.horizen.box.WithdrawalRequestBox
import com.horizen.consensus.ConsensusEpochInfo
import com.horizen.consensus.ConsensusEpochNumber
import com.horizen.utils.{BlockFeeInfo, WithdrawalEpochInfo}
import scorex.util.ModifierId

trait StateReader extends scorex.core.transaction.state.StateReader {

  def withdrawalRequests(withdrawalEpoch: Int): Seq[WithdrawalRequest]
  def certificate(referencedWithdrawalEpoch: Int): Option[WithdrawalEpochCertificate]
  def certificateTopQuality(referencedWithdrawalEpoch: Int): Long
  def getWithdrawalEpochInfo: WithdrawalEpochInfo

  def hasCeased: Boolean

  def getConsensusEpochNumber: Option[ConsensusEpochNumber]

  // todo: fee payments related part
  def getBlockFeePayments(withdrawalEpochNumber: Int): Seq[BlockFeeInfo]
}