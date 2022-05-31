package com.horizen.account.forger

import akka.actor.{ActorRef, ActorSystem, Props}
import com.horizen._
import com.horizen.account.companion.SidechainAccountTransactionsCompanion
import com.horizen.account.history.AccountHistory
import com.horizen.account.mempool.AccountMemoryPool
import com.horizen.account.state.AccountState
import com.horizen.account.storage.AccountHistoryStorage
import com.horizen.account.wallet.AccountWallet
import com.horizen.forge.{AbstractForger, MainchainSynchronizer}
import com.horizen.params.NetworkParams

import scorex.core.utils.NetworkTimeProvider


class AccountForger(settings: SidechainSettings,
             viewHolderRef: ActorRef,
             forgeMessageBuilder: AccountForgeMessageBuilder,
             timeProvider: NetworkTimeProvider,
             params: NetworkParams) extends AbstractForger(
  settings, viewHolderRef, forgeMessageBuilder, timeProvider, params
) {
  override type HSTOR = AccountHistoryStorage
  override type HIS = AccountHistory
  override type MS = AccountState
  override type VL = AccountWallet
  override type MP = AccountMemoryPool
}

object AccountForgerRef {
  def props(settings: SidechainSettings,
            viewHolderRef: ActorRef,
            mainchainSynchronizer: MainchainSynchronizer,
            companion: SidechainAccountTransactionsCompanion,
            timeProvider: NetworkTimeProvider,
            params: NetworkParams): Props = {
    val forgeMessageBuilder: AccountForgeMessageBuilder = new AccountForgeMessageBuilder(mainchainSynchronizer, companion, params, settings.websocket.allowNoConnectionInRegtest)

    Props(new AccountForger(settings, viewHolderRef, forgeMessageBuilder, timeProvider, params))
  }

  def apply(settings: SidechainSettings,
            viewHolderRef: ActorRef,
            mainchainSynchronizer: MainchainSynchronizer,
            companion: SidechainAccountTransactionsCompanion,
            timeProvider: NetworkTimeProvider,
            params: NetworkParams)
           (implicit system: ActorSystem): ActorRef = system.actorOf(props(settings, viewHolderRef, mainchainSynchronizer, companion, timeProvider, params))

  def apply(name: String,
            settings: SidechainSettings,
            viewHolderRef: ActorRef,
            mainchainSynchronizer: MainchainSynchronizer,
            companion: SidechainAccountTransactionsCompanion,
            timeProvider: NetworkTimeProvider,
            params: NetworkParams)
           (implicit system: ActorSystem): ActorRef = system.actorOf(props(settings, viewHolderRef, mainchainSynchronizer, companion, timeProvider, params), name)
}
