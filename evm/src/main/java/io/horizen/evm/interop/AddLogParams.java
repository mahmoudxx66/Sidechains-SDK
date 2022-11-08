package io.horizen.evm.interop;

import io.horizen.evm.utils.Address;
import io.horizen.evm.utils.Hash;

public class AddLogParams extends HandleParams {
    public Address address;
    public Hash[] topics;
    public byte[] data;

    public AddLogParams() {
    }

    public AddLogParams(int handle, EvmLog evmLog) {
        super(handle);
        this.address = evmLog.address;
        this.topics = evmLog.topics;
        this.data = evmLog.data;
    }
}
