package com.horizen.block


import com.horizen.params.NetworkParams
import com.horizen.utils.{MerkleTree, Utils}
import com.horizen.validation.{InconsistentSidechainBlockDataException, InvalidSidechainBlockDataException}
import scorex.core.block.Block
import scorex.core.block.Block.Timestamp
import com.horizen.transaction.Transaction
import scorex.core.ModifierTypeId
import scorex.util.ModifierId

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._


abstract class SidechainBlockBase[TX <: Transaction, H <: SidechainBlockHeaderBase] ()
  extends OmmersContainer[H] with Block[TX]
{
  override val mainchainHeaders: Seq[MainchainHeader]
  override val ommers: Seq[Ommer[H]]
  val header: H
  val sidechainTransactions: Seq[TX]
  val mainchainBlockReferencesData: Seq[MainchainBlockReferenceData]

  val topQualityCertificateOpt: Option[WithdrawalEpochCertificate] = mainchainBlockReferencesData.flatMap(_.topQualityCertificate).lastOption

  override lazy val version: Block.Version = header.version

  override lazy val timestamp: Timestamp = header.timestamp

  override lazy val parentId: ModifierId = header.parentId

  override val modifierTypeId: ModifierTypeId = SidechainBlockBase.ModifierTypeId

  override lazy val id: ModifierId = header.id
  
  override def toString: String = s"${getClass.getSimpleName}(id = $id)"

  def feePaymentsHash: Array[Byte] = header.feePaymentsHash

  // Check block version
  def versionIsValid(): Boolean

  // Verify that included sidechainTransactions are consistent to header.sidechainTransactionsMerkleRootHash.
  @throws(classOf[InconsistentSidechainBlockDataException])
  protected def verifyTransactionsDataConsistency(): Unit

  // Check that Block data is consistent to Block Header
  protected def verifyDataConsistency(params: NetworkParams): Try[Unit] = Try {
    verifyTransactionsDataConsistency()

    // Verify that included mainchainBlockReferencesData and MainchainHeaders are consistent to header.mainchainMerkleRootHash.
    if(mainchainHeaders.isEmpty && mainchainBlockReferencesData.isEmpty) {
      if(!header.mainchainMerkleRootHash.sameElements(Utils.ZEROS_HASH))
        throw new InconsistentSidechainBlockDataException(s"${getClass.getSimpleName} $id contains inconsistent Mainchain data.")
    } else {
      // Calculate Merkle root hashes of mainchainBlockReferences Data
      val mainchainReferencesDataMerkleRootHash = if (mainchainBlockReferencesData.isEmpty)
        Utils.ZEROS_HASH
      else {
        val merkleTree = MerkleTree.createMerkleTree(mainchainBlockReferencesData.map(_.headerHash).asJava)
        // Check that MerkleTree was not mutated.
        if(merkleTree.isMutated)
          throw new InconsistentSidechainBlockDataException(s"${getClass.getSimpleName} $id MainchainBlockReferencesData leads to mutated MerkleTree.")
        merkleTree.rootHash()
      }

      // Calculate Merkle root hash of MainchainHeaders
      val mainchainHeadersMerkleRootHash = if (mainchainHeaders.isEmpty)
        Utils.ZEROS_HASH
      else {
        val merkleTree = MerkleTree.createMerkleTree(mainchainHeaders.map(_.hash).asJava)
        // Check that MerkleTree was not mutated.
        if(merkleTree.isMutated)
          throw new InconsistentSidechainBlockDataException(s"${getClass.getSimpleName} $id MainchainHeaders lead to mutated MerkleTree.")
        merkleTree.rootHash()
      }

      // Calculate final root hash, that takes as leaves two previously calculated root hashes.
      // Note: no need to check that MerkleTree is not mutated.
      val calculatedMerkleRootHash = MerkleTree.createMerkleTree(
        Seq(mainchainReferencesDataMerkleRootHash, mainchainHeadersMerkleRootHash).asJava
      ).rootHash()

      if (!header.mainchainMerkleRootHash.sameElements(calculatedMerkleRootHash))
        throw new InconsistentSidechainBlockDataException(s"${getClass.getSimpleName} $id contains inconsistent Mainchain data.")
    }


    // Verify that included ommers are consistent to header.ommersMerkleRootHash
    if(ommers.isEmpty) {
      if(!header.ommersMerkleRootHash.sameElements(Utils.ZEROS_HASH))
        throw new InconsistentSidechainBlockDataException(s"${getClass.getSimpleName} $id contains inconsistent Ommers.")
    } else {
      val merkleTree = MerkleTree.createMerkleTree(ommers.map(_.id).asJava)
      val calculatedMerkleRootHash = merkleTree.rootHash()
      if(!header.ommersMerkleRootHash.sameElements(calculatedMerkleRootHash))
        throw new InconsistentSidechainBlockDataException(s"${getClass.getSimpleName} $id contains inconsistent Ommers.")

      // Check that MerkleTree was not mutated.
      if(merkleTree.isMutated)
        throw new InconsistentSidechainBlockDataException(s"${getClass.getSimpleName} $id Ommers lead to mutated MerkleTree.")
    }

    // Check ommers data consistency
    for(ommer <- ommers) {
      ommer.verifyDataConsistency() match {
        case Success(_) =>
        case Failure(e) => throw e
      }
    }
  }

  def semanticValidity(params: NetworkParams): Try[Unit] = Try {
    // version is specific to block subclass
    if(!versionIsValid())
      throw new InvalidSidechainBlockDataException(s"${getClass.getSimpleName} $id version $version is invalid.")

    // Check that header is valid.
    header.semanticValidity(params) match {
      case Success(_) =>
      case Failure(e) => throw e
    }

    // Check that body is consistent to header.
    verifyDataConsistency(params) match {
      case Success(_) =>
      case Failure(e) => throw e
    }

    if(sidechainTransactions.size > SidechainBlockBase.MAX_SIDECHAIN_TXS_NUMBER)
      throw new InvalidSidechainBlockDataException(s"${getClass.getSimpleName} $id sidechain transactions amount exceeds the limit.")

    // Check Block size
    val blockSize: Int = bytes.length
    if(blockSize > SidechainBlockBase.MAX_BLOCK_SIZE)
      throw new InvalidSidechainBlockDataException(s"${getClass.getSimpleName} $id size exceeds the limit.")


    // Check MainchainHeaders order in current block.
    for(i <- 0 until mainchainHeaders.size - 1) {
      if(!mainchainHeaders(i).isParentOf(mainchainHeaders(i+1)))
        throw new InvalidSidechainBlockDataException(s"${getClass.getSimpleName} $id MainchainHeader ${mainchainHeaders(i).hashHex} is not a parent of MainchainHeader ${mainchainHeaders(i+1)}.")
    }

    // Check that SidechainTransactions are valid.
    for(tx <- sidechainTransactions) {
      Try {
        tx.semanticValidity()
      } match {
        case Success(_) =>
        case Failure(e) => throw new InvalidSidechainBlockDataException(
          s"${getClass.getSimpleName} $id Transaction ${tx.id} is semantically invalid: ${e.getMessage}.")
      }
    }

    // Check that MainchainHeaders are valid.
    for(mainchainHeader <- mainchainHeaders) {
      mainchainHeader.semanticValidity(params) match {
        case Success(_) =>
        case Failure(e) => throw e
      }
    }

    // Check Ommers
    verifyOmmersSeqData(params) match {
      case Success(_) =>
      case Failure(e) => throw e
    }
  }
}


object SidechainBlockBase {
  // SC Max block size is enough to include at least 2 MC block ref data full of SC outputs + Top quality cert -> ~2.3MB each
  // Also it is more than enough to process Ommers for very long MC forks (2000+)
  val MAX_BLOCK_SIZE: Int = 5000000
  val MAX_SIDECHAIN_TXS_NUMBER: Int = 1000
  val ModifierTypeId: ModifierTypeId = scorex.core.ModifierTypeId @@ 3.toByte

  def calculateMainchainMerkleRootHash(mainchainBlockReferencesData: Seq[MainchainBlockReferenceData],
                                       mainchainHeaders: Seq[MainchainHeader]): Array[Byte] = {
    if(mainchainBlockReferencesData.isEmpty && mainchainHeaders.isEmpty)
      Utils.ZEROS_HASH
    else {
      // Calculate Merkle root hashes of mainchainBlockReferences Data
      val mainchainReferencesDataMerkleRootHash = if(mainchainBlockReferencesData.isEmpty)
        Utils.ZEROS_HASH
      else
        MerkleTree.createMerkleTree(mainchainBlockReferencesData.map(_.headerHash).asJava).rootHash()

      // Calculate Merkle root hash of MainchainHeaders
      val mainchainHeadersMerkleRootHash = if(mainchainHeaders.isEmpty)
        Utils.ZEROS_HASH
      else
        MerkleTree.createMerkleTree(mainchainHeaders.map(_.hash).asJava).rootHash()

      // Calculate final root hash, that takes as leaves two previously calculated root hashes.
      MerkleTree.createMerkleTree(
        Seq(mainchainReferencesDataMerkleRootHash, mainchainHeadersMerkleRootHash).asJava
      ).rootHash()
    }
  }

  def calculateOmmersMerkleRootHash[H <: SidechainBlockHeaderBase](ommers: Seq[Ommer[H]]): Array[Byte] = {
    if(ommers.nonEmpty)
      MerkleTree.createMerkleTree(ommers.map(_.id).asJava).rootHash()
    else
      Utils.ZEROS_HASH
  }
}