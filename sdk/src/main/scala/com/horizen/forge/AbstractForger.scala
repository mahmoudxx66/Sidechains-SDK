package com.horizen.forge

import java.util.{Timer, TimerTask}
import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import com.horizen.{Wallet, _}
import com.horizen.block.{SidechainBlockBase, SidechainBlockHeaderBase}
import com.horizen.consensus.{ConsensusEpochAndSlot, ConsensusEpochNumber, ConsensusSlotNumber}
import com.horizen.forge.AbstractForger.ReceivableMessages.{GetForgingInfo, StartForging, StopForging, TryForgeNextBlockForEpochAndSlot}
import com.horizen.params.NetworkParams
import com.horizen.storage.AbstractHistoryStorage
import com.horizen.transaction.Transaction
import com.horizen.utils.TimeToEpochUtils
import scorex.core.NodeViewHolder.{CurrentView, ReceivableMessages}
import scorex.core.NodeViewHolder.ReceivableMessages.LocallyGeneratedModifier
import scorex.core.transaction.MemoryPool
import scorex.core.transaction.state.MinimalState
import scorex.core.utils.NetworkTimeProvider
import scorex.util.ScorexLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

abstract class AbstractForger[
  TX <: Transaction,
  H <: SidechainBlockHeaderBase,
  PM <: SidechainBlockBase[TX, H]
](settings: SidechainSettings,
             viewHolderRef: ActorRef,
             forgeMessageBuilder: AbstractForgeMessageBuilder[TX, H, PM],
             timeProvider: NetworkTimeProvider,
             val params: NetworkParams) extends Actor with ScorexLogging
{
  type HSTOR <: AbstractHistoryStorage[PM, HSTOR]
  type HIS <: AbstractHistory[TX, H, PM, HSTOR, HIS]
  type MS <: MinimalState[PM, MS]
  type VL <: Wallet[SidechainTypes#SCS, SidechainTypes#SCP, TX, PM, VL]
  type MP <: MemoryPool[TX, MP]

  type View = CurrentView[HIS, MS, VL, MP]

  val timeoutDuration: FiniteDuration = settings.scorexSettings.restApi.timeout
  implicit val timeout: Timeout = Timeout(timeoutDuration)

  private val consensusMillisecondsInSlot: Int = params.consensusSecondsInSlot * 1000
  private def forgingInitiatorTimerTask: TimerTask = new TimerTask {override def run(): Unit = tryToCreateBlockNow()}
  private var timerOpt: Option[Timer] = None

  private def startTimer(): Unit = {
    this.timerOpt match {
      case Some(_) => log.info("Automatically forging already had been started")
      case None => {
        val newTimer = new Timer()
        newTimer.scheduleAtFixedRate(forgingInitiatorTimerTask, 0, consensusMillisecondsInSlot)
        timerOpt = Some(newTimer)
        log.info("Automatically forging had been started")
      }
    }
  }

  private def stopTimer(): Unit = {
    this.timerOpt match {
      case Some(timer) => {
        timer.cancel()
        log.info("Automatically forging had been stopped")
        this.timerOpt = None
      }
      case None => log.info("Automatically forging had been already stopped")
    }
  }

  protected def isForgingEnabled: Boolean = {
    timerOpt.isDefined
  }

  override def preStart(): Unit = {
    context.system.eventStream.subscribe(self, SidechainAppEvents.SidechainApplicationStart.getClass)
  }

  override def receive: Receive = {
    checkForger orElse
    processStartForgingMessage orElse
    processStopForgingMessage orElse
    processTryForgeNextBlockForEpochAndSlotMessage orElse
    processGetForgeInfo orElse {
      case message: Any => log.error(s"Forger received strange message: ${message} from ${sender().path.name}")
    }
  }

  protected def checkForger: Receive = {
    case SidechainAppEvents.SidechainApplicationStart =>
      if(settings.forger.automaticForging)
        self ! StartForging
  }

  protected def processStartForgingMessage: Receive = {
    case StartForging => {
      log.info("Receive StartForging message")
      startTimer()
      // Don't send answer to itself.
      if(sender() != self)
        sender() ! Success(Unit)
    }
  }

  protected def processStopForgingMessage: Receive = {
    case StopForging => {
      log.info("Receive StopForging message")
      stopTimer()
      sender() ! Success(Unit)
    }
  }

  protected def processTryForgeNextBlockForEpochAndSlotMessage: Receive = {
    case TryForgeNextBlockForEpochAndSlot(epochNumber, slotNumber) => tryToCreateBlockForEpochAndSlot(epochNumber, slotNumber, Some(sender()), timeout)
  }

  protected def tryToCreateBlockNow(): Unit = {
    val currentTime: Long = timeProvider.time() / 1000
    val epochAndSlot = TimeToEpochUtils.timestampToEpochAndSlot(params, currentTime)
    log.info(s"Send TryForgeNextBlockForEpochAndSlot message with epoch and slot ${epochAndSlot}")
    tryToCreateBlockForEpochAndSlot(epochAndSlot.epochNumber, epochAndSlot.slotNumber, None, timeout)
  }

  def getForgedBlockAsFuture(epochNumber: ConsensusEpochNumber, slot: ConsensusSlotNumber, blockCreationTimeout: Timeout) : Future[ForgeResult] = {
    val forgeMessage: AbstractForgeMessageBuilder[TX, H, PM]#ForgeMessageType = forgeMessageBuilder.buildForgeMessageForEpochAndSlot(epochNumber, slot, blockCreationTimeout)
    val forgedBlockAsFuture = (viewHolderRef ? forgeMessage).asInstanceOf[Future[ForgeResult]]
    forgedBlockAsFuture
  }

  protected def tryToCreateBlockForEpochAndSlot(epochNumber: ConsensusEpochNumber, slot: ConsensusSlotNumber, respondsToOpt: Option[ActorRef], blockCreationTimeout: Timeout): Unit = {

    val forgedBlockAsFuture = getForgedBlockAsFuture(epochNumber, slot, blockCreationTimeout)

    forgedBlockAsFuture.onComplete{
      case Success(ForgeSuccess(block)) => {
        log.info(s"Got successfully forged block with id ${block.id}")
        viewHolderRef ! LocallyGeneratedModifier(block)
        respondsToOpt.map(respondsTo => respondsTo ! Success(block.id))
      }

      case Success(SkipSlot(reason)) => {
        log.info(s"Slot is skipped with reason: $reason")
        respondsToOpt.map(respondsTo => respondsTo ! Failure(new RuntimeException(s"Slot had been skipped with reason: $reason")))
      }

      case Success(NoOwnedForgingStake) => {
        log.info(s"No forging stake.")
        respondsToOpt.map(respondsTo => respondsTo ! Failure(new RuntimeException("Can't forge block, no forging stake is present for epoch.")))
      }

      case Success(ForgeFailed(ex)) => {
        log.error(s"Forging had been failed. Reason: ${ex.getMessage}")
        respondsToOpt.map(respondsTo => respondsTo ! Failure(ex))
      }

      case failure @ Failure(ex) => {
        log.error(s"Forging had been failed. Reason: ${ex.getMessage}")
        respondsToOpt.map(respondsTo => respondsTo ! failure)
      }
    }
  }

  //protected def processGetForgeInfo: Receive
  protected def processGetForgeInfo: Receive = {
    case GetForgingInfo =>
      val forgerInfoRequester = sender()

      val getInfoMessage
      = ReceivableMessages.GetDataFromCurrentView[HIS, MS, VL, MP, ConsensusEpochAndSlot](getEpochAndSlotForBestBlock)
      val epochAndSlotFut = (viewHolderRef ? getInfoMessage).asInstanceOf[Future[ConsensusEpochAndSlot]]
      epochAndSlotFut.onComplete {
        case Success(epochAndSlot: ConsensusEpochAndSlot) =>
          forgerInfoRequester ! Success(ForgingInfo(params.consensusSecondsInSlot, params.consensusSlotsInEpoch, epochAndSlot, isForgingEnabled))

        case failure@Failure(ex) =>
          forgerInfoRequester ! failure

      }
  }

  def getEpochAndSlotForBestBlock(view: View): ConsensusEpochAndSlot = {
    val history = view.history
    TimeToEpochUtils.timestampToEpochAndSlot(params, history.bestBlockInfo.timestamp)
  }
}

object AbstractForger extends ScorexLogging {
  object ReceivableMessages {
    case object StartForging
    case object StopForging
    case class  TryForgeNextBlockForEpochAndSlot(consensusEpochNumber: ConsensusEpochNumber, consensusSlotNumber: ConsensusSlotNumber)
    case object GetForgingInfo
  }
}