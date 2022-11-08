package io.horizen.evm.interop;

import io.horizen.evm.utils.Hash;

public class SetStorageParams extends StorageParams {
    public Hash value;

    public SetStorageParams() {
    }

    public SetStorageParams(int handle, byte[] address, byte[] key, byte[] value) {
        super(handle, address, key);
        this.value = Hash.FromBytes(value);
    }
}

