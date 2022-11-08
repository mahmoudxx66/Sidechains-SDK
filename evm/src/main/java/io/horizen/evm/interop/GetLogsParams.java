package io.horizen.evm.interop;

import io.horizen.evm.utils.Hash;

public class GetLogsParams extends HandleParams {
    public Hash txHash;

    public GetLogsParams() {
    }

    public GetLogsParams(int handle, byte[] txHash) {
        super(handle);
        this.txHash = Hash.FromBytes(txHash);
    }
}
