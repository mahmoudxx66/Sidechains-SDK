package io.horizen.evm.interop;

import io.horizen.evm.utils.Hash;

public class SetTxContextParams extends HandleParams {
    public Hash txHash;
    public Integer txIndex;

    public SetTxContextParams() {
    }

    public SetTxContextParams(int handle, byte[] txHash, Integer txIndex) {
        super(handle);
        this.txHash = Hash.FromBytes(txHash);
        this.txIndex = txIndex;
    }
}
