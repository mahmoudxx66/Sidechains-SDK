#!/usr/bin/env python3

import time
from SidechainTestFramework.sc_test_framework import SidechainTestFramework
from SidechainTestFramework.sc_boostrap_info import SCNodeConfiguration, SCCreationInfo, MCConnectionInfo, \
    SCNetworkConfiguration
from test_framework.util import initialize_chain_clean, start_nodes, \
    websocket_port_by_mc_node_index, connect_nodes_bi, disconnect_nodes_bi, assert_equal, forward_transfer_to_sidechain, \
    sync_blocks
from SidechainTestFramework.scutil import bootstrap_sidechain_nodes, start_sc_nodes, generate_next_blocks


"""

Configuration:
    Start 2 MC nodes and 1 SC node (with default websocket configuration).
    SC node connected to the first MC node. SC is a Certificate submitter and a signer.
    MC nodes are connected.

Test:
    - Synchronize MC nodes to the point of SC Creation Block.
    - Mine MC blocks to reach the last but one block of the withdrawal epoch 
    - Forge SC block. 
    - Disconnect MC nodes.
    - Create a fork on MC, mining 2 blocks on MC node 1 and 3 blocks on MC node 2. On node 1, a Forward Transfer is 
      also created in order to have different cumulative trees on node 1 and node 2.
    - Forge 2 SC blocks, in order to start the submission window and the certificate generation.
    - Wait until the certificate proof generation is started then connect and synchronize MC nodes 1 and 2. 
    - Forge 3 SC blocks, in order to generate the fork on the SC too and start the generation of the new certificate.
    - Look for the certificate in MC mempool. If it is still not present, try to forge 2 additional SC, just to see if 
      the new blocks are able to create the certificate.
     
    MC blocks on MC node 1 in the end:
    468     -   469     -   470
        \
            -   469'    -   470'    -   471   
            
            
    SC Block on SC node in the end: <sc block/slot number>[<mc headers included>; <mc refdata included>; <ommers>]
    G[420h;420d;] - 0[;;] - 1[421h;421d;]
                          \
                                - 2[421'h,422'h;;1[...]] - 3[423'h,424'h;421'd-424'd;] - 4[;;]
"""


class MCSCForging5(SidechainTestFramework):

    number_of_mc_nodes = 2
    number_of_sidechain_nodes = 1
    sc_withdrawal_epoch_length = 20

    def __init__(self):
        self.sc_nodes_bootstrap_info = None
        self.nodes = None

    def setup_chain(self):
        initialize_chain_clean(self.options.tmpdir, self.number_of_mc_nodes)

    def setup_network(self, split=False):
        # Setup nodes and connect them
        self.nodes = self.setup_nodes()
        connect_nodes_bi(self.nodes, 0, 1)
        self.sync_all()

    def setup_nodes(self):
        # Start 3 MC nodes
        return start_nodes(self.number_of_mc_nodes, self.options.tmpdir, extra_args=[['-debug=sc', '-debug=ws', '-debug=cert',  '-logtimemicros=1', '-scproofqueuesize=0']] * self.number_of_mc_nodes)

    def sc_setup_chain(self):
        # Bootstrap new SC, specify SC node 1 connection to MC node 1
        mc_node_1 = self.nodes[0]
        sc_node_1_configuration = SCNodeConfiguration(
            MCConnectionInfo(address="ws://{0}:{1}".format(mc_node_1.hostname, websocket_port_by_mc_node_index(0))),
            True,  # Certificate submission is enabled
            True,  # Certificate signing is enabled
            [0, 1, 2, 3, 4, 5, 6]  # owns 3 schnorr PKs for certificate signing
        )

        network = SCNetworkConfiguration(SCCreationInfo(mc_node_1, 600, self.sc_withdrawal_epoch_length),
                                         sc_node_1_configuration)
        self.sc_nodes_bootstrap_info = bootstrap_sidechain_nodes(self.options, network)

    def sc_setup_nodes(self):
        # Start 1 SC node
        return start_sc_nodes(self.number_of_sidechain_nodes, self.options.tmpdir)

    def run_test(self):
        # Synchronize mc_node1 and mc_node2, then disconnect them.
        self.sync_all()

        mc_node1 = self.nodes[0]
        mc_node2 = self.nodes[1]
        sc_node1 = self.sc_nodes[0]

        # Generate enough blocks to arrive til withdrawal epoch length - 1 block.
        mc_blocks_left_for_we = self.sc_withdrawal_epoch_length - 1  # minus genesis block
        # Generate MC blocks to reach one block before the end of the withdrawal epoch (WE)
        mc_node1.generate(mc_blocks_left_for_we - 1)
        self.sync_nodes([mc_node1, mc_node2])

        # Disconnect MC node 2, in order to create a fork
        disconnect_nodes_bi(self.nodes, 0, 1)

        # Generate a SC block, linking withdrawal epoch length - 1 MC blocks
        generate_next_blocks(sc_node1, "first node", 1)

        print("Creating the fork on MC")
        print("Creating a FT on MC node 1 so the Cumulative tree is different")
        mc1_return_address = mc_node1.getnewaddress()

        forward_transfer_to_sidechain(self.sc_nodes_bootstrap_info.sidechain_id,
                                                                  mc_node1,
                                                                  self.sc_nodes_bootstrap_info.genesis_account.publicKey,
                                                                  1,
                                                                  mc1_return_address,
                                                                  False)

        # Waiting for the FT in the mempool
        while mc_node1.getmempoolinfo()["size"] == 0:
            time.sleep(1)
        # Generate the last withdrawal epoch block and the first on submission window on MC node 1
        mc_node1.generate(2)

        print("Generate another 3 MC blocks on the MC node 2, they will become the new active chain")
        mc_node2.generate(3)

        # Generate 2 SC blocks for starting certificate generation.
        generate_next_blocks(sc_node1, "first node", 2)

        # Wait for certificate generation start
        while not sc_node1.submitter_isCertGenerationActive()["result"]["state"]:
            print("Wait for certificate generation start...")
            time.sleep(1)
        time.sleep(21)

        # Connect and synchronize MC node 1 to MC node 2
        connect_nodes_bi(self.nodes, 0, 1)
        #I just wait for block sync because the mem pools won't be synchronized. This is because in node 1 mem pool will remain the FT, after the chain switch
        sync_blocks([mc_node1, mc_node2])

        # Generate 2 SC blocks to reach the new withdrawal epoch end
        generate_next_blocks(sc_node1, "first node", 2)

        # Generate SC block => first block in submission window
        generate_next_blocks(sc_node1, "first node", 1)

        # Check if in MC mempool there is a certificate
        while mc_node1.getmempoolinfo()["size"] == 1 and sc_node1.submitter_isCertGenerationActive()["result"]["state"]:
            print("Wait for certificate in mc mempool...")
            time.sleep(2)

        if mc_node1.getmempoolinfo()["size"] == 1:
            print("No certificate in MC mempool. Trying to create additional SC blocks to see if it is created")
            generate_next_blocks(sc_node1, "first node", 2)
            time.sleep(15)
            # Check if in MC mempool there is a certificate
            while mc_node1.getmempoolinfo()["size"] == 1 and sc_node1.submitter_isCertGenerationActive()["result"]["state"]:
                print("Wait for certificate in mc mempool...")
                time.sleep(2)

        assert_equal(2, mc_node1.getmempoolinfo()["size"], "Certificate was not added to Mc node mempool.")
        print("OK ")





if __name__ == "__main__":
    MCSCForging5().main()