package io.horizen.evm.interop;

import io.horizen.evm.utils.Address;
import io.horizen.evm.utils.Hash;

public class StorageParams extends AccountParams {
    public Hash key;

    public StorageParams() {
    }

    public StorageParams(int handle, byte[] address, byte[] key) {
        super(handle, address);
        this.address = Address.FromBytes(address);
        this.key = Hash.FromBytes(key);
    }
}
