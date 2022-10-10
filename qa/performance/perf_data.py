from __future__ import annotations
from enum import Enum


class NetworkTopology(Enum):
    DaisyChain = "daisy_chain"
    Ring = "ring"
    Star = "star"
    PeerToPeer = "p2p"
    StrongConnectedForgers = "strong_connected_forgers"


class TestType(Enum):
    Mempool = "mempool"
    Mempool_Timed = "mempool_timed"
    Transactions_Per_Second = "transactions_per_second"
    All_Transactions = "all_transactions"